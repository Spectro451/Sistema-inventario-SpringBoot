package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.VentaMueble;
import com.muebleria.inventario.repository.VentaMuebleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VentaMuebleService {

    @Autowired
    VentaMuebleRepository ventaMuebleRepository;

   public List<VentaMueble> findAll() {
       return ventaMuebleRepository.findAll();
   }

   public Optional<VentaMueble> getById(Long id) {
       return ventaMuebleRepository.findById(id);
   }

   public List<VentaMueble> obtenerPorMuebleId(Long muebleId) {
       return ventaMuebleRepository.findByMuebleId(muebleId);
   }

    public List<VentaMueble> obtenerPorVentaId(Long ventaId) {
        return ventaMuebleRepository.findByVentaId(ventaId);
    }

    public VentaMueble guardar(VentaMueble ventaMueble) {
        return ventaMuebleRepository.save(ventaMueble);
    }

    public void deleteById(Long id) {
        ventaMuebleRepository.deleteById(id);
    }
}
