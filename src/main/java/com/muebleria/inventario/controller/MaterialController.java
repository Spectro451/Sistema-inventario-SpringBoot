package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/materiales")
public class MaterialController {

    @Autowired
    MaterialService materialService;

    @GetMapping
    public List<Material> findAll() {
        return materialService.mostrarTodos();
    }

    @PostMapping
    public Material guardar(@RequestBody Material material) {
        materialService.guardar(material);
        return material;
    }

    @GetMapping("/{id}")
    public Optional<Material> buscarId(@PathVariable("id") Long id) {
        return materialService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable("id") Long id) {
        materialService.eliminar(id);
    }
}
