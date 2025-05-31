package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1/proveedor")
public class ProveedorController {

    @Autowired
    ProveedorService proveedorService;

    @GetMapping
    public List<Proveedor> getall() {
        return proveedorService.getProveedores();
    }

    @PostMapping
    public Proveedor guardar(@RequestBody Proveedor proveedor) {
        proveedorService.guardarProveedor(proveedor);
        return proveedor;
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable("id") Long id){
        proveedorService.eliminarProveedor(id);
    }

    @GetMapping("/{id}")
    public Optional<Proveedor> getProveedorId(@PathVariable("id") Long id){
        return proveedorService.getProveedoresId(id);
    }
}
