package com.muebleria.inventario.controller;

import com.muebleria.inventario.dto.ProveedorDTO;
import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1/proveedor")
public class ProveedorController {

    @Autowired
    ProveedorService proveedorService;

//    @GetMapping
//    public List<Proveedor> getall() {
//        return proveedorService.getProveedores();
//    }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Proveedor proveedor) {
        try {
            Proveedor creado = proveedorService.guardarProveedor(proveedor);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (RuntimeException ex) {
            // Devolvemos el mensaje de la excepción para que Postman lo vea
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            proveedorService.eliminarProveedor(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Optional<Proveedor> getProveedorId(@PathVariable("id") Long id){
        return proveedorService.getProveedoresId(id);
    }
    @GetMapping
    public List<ProveedorDTO> listarProveedores() {
        return proveedorService.findAllDTO();
    }
}
