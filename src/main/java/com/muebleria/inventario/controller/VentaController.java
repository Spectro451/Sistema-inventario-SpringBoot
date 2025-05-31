package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.entidad.Venta;
import com.muebleria.inventario.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1/venta")
public class VentaController {

    @Autowired
    VentaService ventaService;

    @GetMapping
    public List<Venta> listar() {
        return ventaService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Venta> buscar(@PathVariable Long id) {
        return ventaService.findById(id);
    }

    @PostMapping
    public Venta guardar(@RequestBody Venta venta) {
        ventaService.guardar(venta);
        return venta;
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable("id") Long id){
        ventaService.eliminar(id);
    }
}
