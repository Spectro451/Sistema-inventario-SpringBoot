package com.muebleria.inventario.service;


import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.repository.MuebleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MuebleService {

    @Autowired
    MuebleRepository muebleRepository;

    @Autowired
    MaterialMuebleService materialMuebleService;

    public List<Mueble> findAll() {

        return muebleRepository.findAll();
    }

    public Optional<Mueble> findById(Long id) {

        return muebleRepository.findById(id);
    }

    public void delete(Long id) {
        if (!muebleRepository.existsById(id)) {
            throw new RuntimeException("Mueble con id " + id + " no existe.");
        }
        muebleRepository.deleteById(id);
    }

    public Mueble update(Long id, Mueble muebleActualizado) {
        Optional<Mueble> optionalMueble = muebleRepository.findById(id);
        if (optionalMueble.isPresent()) {
            Mueble muebleActual = optionalMueble.get();

            muebleActual.setNombre(muebleActualizado.getNombre());
            muebleActual.setDescripcion(muebleActualizado.getDescripcion());
            muebleActual.setPrecioVenta(muebleActualizado.getPrecioVenta());
            muebleActual.setStock(muebleActualizado.getStock());

            return muebleRepository.save(muebleActual);
        }
        else {
            throw new RuntimeException("No se encontro el mueble");
        }
    }

    @Transactional
    public Mueble guardarConDetalle(Mueble mueble) {
        Mueble muebleSolo = new Mueble();
        muebleSolo.setNombre(mueble.getNombre());
        muebleSolo.setDescripcion(mueble.getDescripcion());
        muebleSolo.setPrecioVenta(mueble.getPrecioVenta());
        muebleSolo.setStock(mueble.getStock());

        Mueble muebleGuardado = muebleRepository.save(muebleSolo);

        List<MaterialMueble> relacionesGuardadas = new ArrayList<>();

        if (mueble.getMaterialMuebles() != null) {
            for (MaterialMueble mm : mueble.getMaterialMuebles()) {
                mm.setMueble(muebleGuardado);
                MaterialMueble guardado = materialMuebleService.guardar(mm);
                relacionesGuardadas.add(guardado);
            }
        }


        muebleGuardado.setMaterialMuebles(relacionesGuardadas);

        return muebleGuardado;
    }
}
