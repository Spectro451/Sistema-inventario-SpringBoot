package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.service.ProveedorMaterialesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/proveedor-materiales")
public class ProveedorMaterialesController {

    @Autowired
    ProveedorMaterialesService proveedorMaterialesService;

    @GetMapping
    public List<ProveedorMateriales> obtenerTodos() {
        return proveedorMaterialesService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public Optional<ProveedorMateriales> obtenerPorId(@PathVariable Long id) {
        return proveedorMaterialesService.obtenerPorId(id);
    }

    @GetMapping("/proveedor/{proveedorId}")
    public List<ProveedorMateriales> obtenerPorProveedor(@PathVariable Long proveedorId) {
        return proveedorMaterialesService.obtenerPorProveedorId(proveedorId);
    }

    @GetMapping("/material/{materialId}")
    public List<ProveedorMateriales> obtenerPorMaterial(@PathVariable Long materialId) {
        return proveedorMaterialesService.obtenerPorMaterialId(materialId);
    }

    @PostMapping
    public ProveedorMateriales guardar(@RequestBody ProveedorMateriales proveedorMateriales) {
        proveedorMaterialesService.guardar(proveedorMateriales);
        return proveedorMateriales;
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        proveedorMaterialesService.eliminar(id);
    }
}
