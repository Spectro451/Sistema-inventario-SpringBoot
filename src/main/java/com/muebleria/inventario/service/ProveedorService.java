package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MaterialSimpleDTO;
import com.muebleria.inventario.dto.ProveedorDTO;
import com.muebleria.inventario.dto.ProveedorMaterialDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import com.muebleria.inventario.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProveedorService {
    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProveedorMaterialesRepository proveedorMaterialesRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ProveedorMaterialesService proveedorMaterialesService;

    public List<Proveedor> getProveedores() {
        return proveedorRepository.findAll();
    }

    public Optional<Proveedor> getProveedoresId(Long id) {
        return proveedorRepository.findById(id);
    }

    @Transactional
    public Proveedor guardarProveedor(Proveedor proveedor) {

        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);

        if (proveedor.getProveedorMateriales() != null) {
            List<ProveedorMateriales> pmGuardados = new ArrayList<>();

            for (ProveedorMateriales pmOriginal : proveedor.getProveedorMateriales()) {
                Long materialId = pmOriginal.getMaterial().getId();


                Material material = materialRepository.findById(materialId)
                        .orElseThrow(() -> new RuntimeException(
                                "Material no encontrado con id: " + materialId
                        ));

                boolean yaExiste = proveedorMaterialesRepository
                        .existsByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), materialId);

                if (yaExiste) {

                    ProveedorMateriales pmExistente = proveedorMaterialesRepository
                            .findByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), materialId);

                    pmGuardados.add(pmExistente);
                    continue;
                }

                ProveedorMateriales pmAux = new ProveedorMateriales();
                pmAux.setProveedor(proveedorGuardado);
                pmAux.setMaterial(material);
                pmAux.setCostoUnitario(pmOriginal.getCostoUnitario());

                ProveedorMateriales pmResult = proveedorMaterialesService.guardar(pmAux);

                pmGuardados.add(pmResult);
            }

            proveedorGuardado.getProveedorMateriales().clear();


            proveedorGuardado.getProveedorMateriales().addAll(pmGuardados);
        }

        return proveedorGuardado;
    }

    public void eliminarProveedor(Long id) {
        if (!proveedorRepository.existsById(id)) {
            throw new RuntimeException("Proveedor con id " + id + " no existe.");
        }
        proveedorRepository.deleteById(id);
    }

    public List<ProveedorDTO> findAllDTO() {
        List<Proveedor> proveedores = proveedorRepository.findAll();

        return proveedores.stream().map(proveedor -> {
            ProveedorDTO proveedorDTO = new ProveedorDTO();
            proveedorDTO.setId(proveedor.getId());
            proveedorDTO.setNombre(proveedor.getNombre());
            proveedorDTO.setTelefono(proveedor.getTelefono());
            proveedorDTO.setCorreo(proveedor.getCorreo());
            proveedorDTO.setDireccion(proveedor.getDireccion());

            if (proveedor.getProveedorMateriales() != null) {
                List<ProveedorMaterialDTO> pmDTOs = proveedor.getProveedorMateriales().stream().map(pm -> {
                    ProveedorMaterialDTO pmDTO = new ProveedorMaterialDTO();
                    pmDTO.setId(pm.getId());
                    pmDTO.setCostoUnitario(pm.getCostoUnitario());

                    Material material = pm.getMaterial();
                    MaterialSimpleDTO materialSimpleDTO = new MaterialSimpleDTO();
                    materialSimpleDTO.setId(material.getId());
                    materialSimpleDTO.setNombre(material.getNombre());
                    materialSimpleDTO.setTipo(material.getTipo().toString()); // Si usas enum
                    materialSimpleDTO.setDescripcion(material.getDescripcion());
                    materialSimpleDTO.setUnidadDeMedida(material.getUnidadDeMedida());
                    materialSimpleDTO.setStockActual(material.getStockActual());

                    pmDTO.setMaterial(materialSimpleDTO);

                    return pmDTO;
                }).collect(Collectors.toList());

                proveedorDTO.setProveedorMateriales(pmDTOs);
            } else {
                proveedorDTO.setProveedorMateriales(new ArrayList<>());
            }

            return proveedorDTO;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Proveedor actualizarProveedor(Long id, Proveedor proveedorActualizado) {
        Proveedor proveedorExistente = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));

        proveedorExistente.setNombre(proveedorActualizado.getNombre());
        proveedorExistente.setTelefono(proveedorActualizado.getTelefono());
        proveedorExistente.setCorreo(proveedorActualizado.getCorreo());
        proveedorExistente.setDireccion(proveedorActualizado.getDireccion());

        // Delegar la actualización de proveedorMateriales
        if (proveedorActualizado.getProveedorMateriales() != null) {
            // Limpiar relaciones actuales
            proveedorExistente.getProveedorMateriales().clear();

            // Guardar cada relación con el servicio dedicado (puede manejar actualización, creación)
            List<ProveedorMateriales> nuevasRelaciones = new ArrayList<>();
            for (ProveedorMateriales pm : proveedorActualizado.getProveedorMateriales()) {
                pm.setProveedor(proveedorExistente); // aseguramos la referencia
                ProveedorMateriales pmGuardado = proveedorMaterialesService.guardarOActualizar(pm);
                nuevasRelaciones.add(pmGuardado);
            }

            proveedorExistente.getProveedorMateriales().addAll(nuevasRelaciones);
        }

        return proveedorRepository.save(proveedorExistente);
    }
}
