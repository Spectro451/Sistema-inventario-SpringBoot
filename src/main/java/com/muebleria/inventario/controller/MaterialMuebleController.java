package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.service.MaterialMuebleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/material-mueble")
public class MaterialMuebleController {

    @Autowired
    MaterialMuebleService materialMuebleService;

    @GetMapping
    public List<MaterialMueble> getMaterialMuebles() {
        return materialMuebleService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<MaterialMueble> getById(@PathVariable Long id) {
        return materialMuebleService.findById(id);
    }

    @GetMapping("/material/{id}")
    public List<MaterialMueble> getByMaterialId(@PathVariable Long id) {
        return materialMuebleService.findByMaterialId(id);
    }

    @GetMapping("/mueble/{id}")
    public List<MaterialMueble> getByMuebleId(@PathVariable Long id) {
        return materialMuebleService.findByMuebleId(id);
    }

    @PostMapping
    public MaterialMueble save(@RequestBody MaterialMueble materialMueble) {
        materialMuebleService.guardar(materialMueble, 1L); // usamos 1 unidad como base
        return materialMueble;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            materialMuebleService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
