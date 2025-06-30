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
    public ResponseEntity<ProveedorDTO> guardar(@RequestBody ProveedorDTO proveedorDTO) {
        try {
            ProveedorDTO creado = proveedorService.guardarProveedor(proveedorDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null); // o mejor manejo de errores
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

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> actualizarProveedor(
            @PathVariable Long id,
            @RequestBody ProveedorDTO proveedorDTO) {

        ProveedorDTO actualizado = proveedorService.actualizarProveedor(id, proveedorDTO);
        return ResponseEntity.ok(actualizado);
    }
}
