package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.VentaMueble;
import com.muebleria.inventario.service.VentaMuebleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/venta-mueble")
public class VentaMuebleController {

    @Autowired
    VentaMuebleService ventaMuebleService;

    @GetMapping
    public List<VentaMueble> getAll() {
        return ventaMuebleService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<VentaMueble> getById(@PathVariable Long id) {
        return ventaMuebleService.getById(id);
    }

    @GetMapping("/mueble/{muebleId}")
    public List<VentaMueble> getByMuebleId(@PathVariable Long muebleId) {
        return ventaMuebleService.obtenerPorMuebleId(muebleId);
    }

    @GetMapping("/venta/{ventaId}")
    public List<VentaMueble> getByVentaId(@PathVariable Long ventaId) {
        return ventaMuebleService.obtenerPorVentaId(ventaId);
    }

    @PostMapping
    public VentaMueble create(@RequestBody VentaMueble ventaMueble) {
        ventaMuebleService.guardar(ventaMueble);
        return ventaMueble;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ventaMuebleService.deleteById(id);
    }
}
