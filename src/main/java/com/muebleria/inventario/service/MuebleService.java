package com.muebleria.inventario.service;


import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MaterialMuebleDTO;
import com.muebleria.inventario.dto.MaterialSimpleDTO;
import com.muebleria.inventario.dto.MuebleDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.repository.MaterialMuebleRepository;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.MuebleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MuebleService {

    @Autowired
    MuebleRepository muebleRepository;

    @Autowired
    MaterialMuebleService materialMuebleService;

    @Autowired
    MaterialRepository materialRepository;

    public List<Mueble> findAll() {

        return muebleRepository.findAll();
    }

    public Optional<Mueble> findById(Long id) {

        return muebleRepository.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        Mueble mueble = muebleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mueble no encontrado con id: " + id));

        if (!mueble.getVentaMuebles().isEmpty()) {
            throw new RuntimeException("No se puede borrar el mueble porque tiene ventas asociadas.");
        }
        // Devolver el stock de materiales usados
        for (MaterialMueble mm : mueble.getMaterialMuebles()) {
            Material material = mm.getMaterial();
            Long cantidad = mm.getCantidadUtilizada();
            material.setStockActual(material.getStockActual() + cantidad);
            materialRepository.save(material);
        }

        // Borrar el mueble (las relaciones se borran en cascada si está configurado)
        muebleRepository.delete(mueble);
    }

    @Transactional
    public Mueble update(Long id, Mueble muebleActualizado) {
        Mueble muebleActual = muebleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el mueble"));

        Long stockViejo = muebleActual.getStock();
        Long stockNuevo = muebleActualizado.getStock();

        // Actualizar campos básicos
        muebleActual.setNombre(muebleActualizado.getNombre());
        muebleActual.setDescripcion(muebleActualizado.getDescripcion());
        muebleActual.setPrecioVenta(muebleActualizado.getPrecioVenta());
        muebleActual.setStock(stockNuevo);

        Long diferencia = stockNuevo - stockViejo;

        if (diferencia != 0) {
            for (MaterialMueble mm : muebleActual.getMaterialMuebles()) {
                Material material = mm.getMaterial();
                Long cantidadPorUnidad = mm.getCantidadUtilizada();
                Long ajuste = cantidadPorUnidad * diferencia;

                // Si diferencia > 0, restamos material (fabricamos más muebles)
                // Si diferencia < 0, sumamos material (menos muebles, materiales liberados)
                Long nuevoStock = material.getStockActual() - ajuste;

                if (nuevoStock < 0) {
                    throw new RuntimeException("Stock insuficiente para material: " + material.getNombre());
                }

                material.setStockActual(nuevoStock);
                materialRepository.save(material);
            }
        }

        return muebleRepository.save(muebleActual);
    }

    @Transactional
    public Mueble guardarConDetalle(Mueble mueble) {
        // Ignoramos cualquier id que venga del cliente
        Mueble muebleSolo = new Mueble();
        muebleSolo.setNombre(mueble.getNombre());
        muebleSolo.setDescripcion(mueble.getDescripcion());
        muebleSolo.setPrecioVenta(mueble.getPrecioVenta());
        muebleSolo.setStock(mueble.getStock());

        // Guardamos mueble nuevo (se crea con id nuevo)
        Mueble muebleGuardado = muebleRepository.save(muebleSolo);

        List<MaterialMueble> relacionesGuardadas = new ArrayList<>();

        if (mueble.getMaterialMuebles() != null) {
            for (MaterialMueble mm : mueble.getMaterialMuebles()) {
                mm.setId(null); // FORZAR creación NUEVA relación, ignorar cualquier id previo
                mm.setMueble(muebleGuardado);
                MaterialMueble guardado = materialMuebleService.guardar(mm); // Aquí se hace la validación y descuento stock
                relacionesGuardadas.add(guardado);
            }
        }

        // Actualizar la lista de relaciones en el mueble guardado (no es obligatorio si solo se usa el id)
        muebleGuardado.setMaterialMuebles(relacionesGuardadas);

        return muebleGuardado;
    }

    public List<MuebleDTO> findAllDTO() {
        List<Mueble> muebles = muebleRepository.findAll();

        return muebles.stream().map(mueble -> {
            MuebleDTO muebleDTO = new MuebleDTO();
            muebleDTO.setId(mueble.getId());
            muebleDTO.setNombre(mueble.getNombre());
            muebleDTO.setDescripcion(mueble.getDescripcion());
            muebleDTO.setPrecioVenta(mueble.getPrecioVenta());
            muebleDTO.setStock(mueble.getStock());

            if (mueble.getMaterialMuebles() != null) {
                List<MaterialMuebleDTO> mmDTOs = mueble.getMaterialMuebles().stream().map(mm -> {
                    MaterialMuebleDTO mmDTO = new MaterialMuebleDTO();
                    mmDTO.setId(mm.getId());
                    mmDTO.setCantidadUtilizada(mm.getCantidadUtilizada());

                    Material material = mm.getMaterial();
                    MaterialSimpleDTO materialDTO = new MaterialSimpleDTO();
                    materialDTO.setId(material.getId());
                    materialDTO.setNombre(material.getNombre());
                    materialDTO.setTipo(material.getTipo().toString());
                    materialDTO.setDescripcion(material.getDescripcion());
                    materialDTO.setUnidadDeMedida(material.getUnidadDeMedida());
                    materialDTO.setStockActual(material.getStockActual());

                    mmDTO.setMaterial(materialDTO);

                    return mmDTO;
                }).collect(Collectors.toList());

                muebleDTO.setMaterialMuebles(mmDTOs);
            } else {
                muebleDTO.setMaterialMuebles(new ArrayList<>());
            }

            return muebleDTO;
        }).collect(Collectors.toList());
    }
}
