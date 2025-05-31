package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class ProveedorMaterialesService {
    @Autowired
    ProveedorMaterialesRepository proveedorMaterialesRepository;

    public List<ProveedorMateriales> obtenerTodos() {
        return proveedorMaterialesRepository.findAll();
    }

    public Optional<ProveedorMateriales> obtenerPorId(Long id) {
        return proveedorMaterialesRepository.findById(id);
    }

    public List<ProveedorMateriales> obtenerPorProveedorId(Long proveedorId) {
        return proveedorMaterialesRepository.findByProveedorId(proveedorId);
    }

    public List<ProveedorMateriales> obtenerPorMaterialId(Long materialId) {
        return proveedorMaterialesRepository.findByMaterialId(materialId);
    }

    public ProveedorMateriales guardar(ProveedorMateriales proveedorMateriales) {
        return proveedorMaterialesRepository.save(proveedorMateriales);
    }

    public void eliminar(Long id) {
        proveedorMaterialesRepository.deleteById(id);
    }

}
