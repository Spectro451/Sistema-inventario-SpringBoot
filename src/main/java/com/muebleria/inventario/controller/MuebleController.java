package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.service.MuebleService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/mueble")
public class MuebleController {

    @Autowired
    MuebleService muebleService;

    @GetMapping
    public List<Mueble> getMuebles() {
        return muebleService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Mueble> getByID(@PathVariable Long id) {
        return muebleService.findById(id);
    }

    @PostMapping
    public ResponseEntity<?> createMueble(@RequestBody Mueble mueble) {
        try{
            Mueble muebleGuardado = muebleService.guardarConDetalle(mueble);
            return ResponseEntity.ok(muebleGuardado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mueble> updateMueble(@RequestBody Mueble muebleActualizado, @PathVariable Long id) {
        try{
            Mueble mueble = muebleService.update(id, muebleActualizado);
            return ResponseEntity.ok(mueble);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            muebleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
