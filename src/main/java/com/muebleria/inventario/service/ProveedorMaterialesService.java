package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import com.muebleria.inventario.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class ProveedorMaterialesService {
    @Autowired
    ProveedorMaterialesRepository proveedorMaterialesRepository;

    @Autowired
    ProveedorRepository proveedorRepository;

    @Autowired
    MaterialRepository materialRepository;

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

    @Transactional
    public ProveedorMateriales guardar(ProveedorMateriales proveedorMateriales) {

        Long proveedorId = proveedorMateriales.getProveedor().getId();
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + proveedorId));

        Long materialId = proveedorMateriales.getMaterial().getId();
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material no encontrado con id: " + materialId));

        boolean existeRelacion = proveedorMaterialesRepository.existsByProveedor_IdAndMaterial_Id(proveedorId, materialId);
        if (existeRelacion) {
            throw new RuntimeException("La relación Proveedor-Material ya existe.");
        }

        proveedorMateriales.setProveedor(proveedor);
        proveedorMateriales.setMaterial(material);

        return proveedorMaterialesRepository.save(proveedorMateriales);
    }

    public void eliminar(Long id) {
        if (!proveedorMaterialesRepository.existsById(id)) {
            throw new RuntimeException("ProveedorMaterial con id " + id + " no existe.");
        }
        proveedorMaterialesRepository.deleteById(id);
    }

    @Transactional
    public ProveedorMateriales guardarOActualizar(ProveedorMateriales pm) {
        Long proveedorId = pm.getProveedor().getId();
        Long materialId = pm.getMaterial().getId();

        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + proveedorId));

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material no encontrado con id: " + materialId));

        // Buscar si ya existe la relación
        ProveedorMateriales pmExistente = proveedorMaterialesRepository
                .findByProveedor_IdAndMaterial_Id(proveedorId, materialId);

        if (pmExistente != null) {
            pmExistente.setCostoUnitario(pm.getCostoUnitario());
            return proveedorMaterialesRepository.save(pmExistente);
        } else {
            pm.setProveedor(proveedor);
            pm.setMaterial(material);
            return proveedorMaterialesRepository.save(pm);
        }
    }

}
