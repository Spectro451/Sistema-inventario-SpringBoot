package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.repository.MaterialMuebleRepository;
import com.muebleria.inventario.repository.MaterialRepository;
import jakarta.transaction.Transactional;
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

    public MaterialMueble guardar(MaterialMueble materialMueble, Long cantidadMuebles) {
        Long materialId = materialMueble.getMaterial().getId();

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material no encontrado id: " + materialId));

        Long cantidadPorUnidad = materialMueble.getCantidadUtilizada();
        Long cantidadTotal = cantidadPorUnidad * cantidadMuebles;

        if (material.getStockActual() < cantidadTotal) {
            throw new RuntimeException("Stock insuficiente de: " + material.getNombre()
                    + ". Stock actual: " + material.getStockActual() + ", requerido: " + cantidadTotal);
        }

        // Descontar el total necesario
        material.setStockActual(material.getStockActual() - cantidadTotal);
        materialRepository.save(material);

        // Guardar la relación (solo con cantidad por unidad)
        materialMueble.setMaterial(material);

        return materialMuebleRepository.save(materialMueble);
    }

    public void eliminar(Long id) {
        if (!materialRepository.existsById(id)) {
            throw new RuntimeException("MaterialMueble con id " + id + " no existe.");
        }
        materialRepository.deleteById(id);
    }

    @Transactional
    public MaterialMueble update(MaterialMueble mm) {
        // Obtenemos el material asociado
        Material material = materialRepository.findById(mm.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material no encontrado id: " + mm.getMaterial().getId()));

        // Obtenemos el viejo MaterialMueble si existe
        Long vieja = 0L;
        MaterialMueble existente = null;
        if (mm.getId() != null) {
            existente = materialMuebleRepository.findById(mm.getId())
                    .orElseThrow(() -> new RuntimeException("Relación no encontrada id: " + mm.getId()));
            vieja = existente.getCantidadUtilizada();
        }

        Long nueva = mm.getCantidadUtilizada();
        Long delta = nueva - vieja;

        System.out.println("[MM‑UPDATE] id=" + mm.getId() + " viejo=" + vieja + " nueva=" + nueva + " delta=" + delta);

        if (delta != 0) {
            // Verificamos si hay stock suficiente
            if (delta > 0 && material.getStockActual() < delta) {
                throw new RuntimeException("Stock insuficiente para el material: " + material.getNombre());
            }

            // Ajustamos stock
            material.setStockActual(material.getStockActual() - delta);
            materialRepository.save(material);
        }

        // Reutilizamos la entidad existente si aplica
        if (existente != null) {
            existente.setCantidadUtilizada(nueva);
            existente.setMaterial(material);
            return materialMuebleRepository.save(existente);
        } else {
            mm.setMaterial(material);
            return materialMuebleRepository.save(mm);
        }
    }
}
