package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    public List<Material> mostrarTodos() {
        return materialRepository.findAll();
    }

    public Optional<Material> buscarPorId(Long id) {
        return materialRepository.findById(id);
    }

    public Material guardar(Material material) {
        return materialRepository.save(material);
    }

    public void eliminar(Long id) {
        materialRepository.deleteById(id);
    }
}
