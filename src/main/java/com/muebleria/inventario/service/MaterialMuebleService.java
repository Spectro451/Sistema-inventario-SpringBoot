package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.repository.MaterialMuebleRepository;
import com.muebleria.inventario.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaterialMuebleService {

    @Autowired
    MaterialMuebleRepository materialMuebleRepository;

    @Autowired
    private MaterialRepository materialRepository;

    public List<MaterialMueble> findAll() {
        return materialMuebleRepository.findAll();
    }

    public Optional<MaterialMueble> findById(Long id) {
        return materialMuebleRepository.findById(id);
    }

    public List<MaterialMueble> findByMaterialId(Long materialId) {
        return materialMuebleRepository.findByMaterialId(materialId);
    }

    public List<MaterialMueble> findByMuebleId(Long muebleId) {
        return materialMuebleRepository.findByMuebleId(muebleId);
    }

    public MaterialMueble guardar(MaterialMueble materialMueble) {
        Long materialId = materialMueble.getMaterial().getId();

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material no encontrado id: " + materialId));

        Long stockActual = material.getStockActual();
        Long cantidadNecesaria = materialMueble.getCantidadUtilizada();

        if (stockActual < cantidadNecesaria) {
            throw new RuntimeException("Stock insuficiente de: " + material.getNombre()
                    + ". Stock actual: " + stockActual + ", requerido: " + cantidadNecesaria);
        }

        material.setStockActual(stockActual - cantidadNecesaria);
        materialRepository.save(material);

        materialMueble.setMaterial(material);

        return materialMuebleRepository.save(materialMueble);

    }

    public void eliminar(Long id) {
        if (!materialRepository.existsById(id)) {
            throw new RuntimeException("MaterialMueble con id " + id + " no existe.");
        }
        materialRepository.deleteById(id);
    }
}
