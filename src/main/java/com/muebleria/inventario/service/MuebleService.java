package com.muebleria.inventario.service;


import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.repository.MuebleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Mueble save(Mueble mueble) {

        return muebleRepository.save(mueble);
    }

    public void delete(Long id) {

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
    public Mueble guardarConMaterial(Mueble mueble) {

        Mueble muebleGuardado = muebleRepository.save(mueble);

        for (MaterialMueble mm : mueble.getMaterialMuebles()) {
            mm.setMueble(muebleGuardado);
            materialMuebleService.guardar(mm);
        }
        return muebleGuardado;
    }
}
