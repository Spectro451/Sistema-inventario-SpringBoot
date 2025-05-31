package com.muebleria.inventario.controller;

import com.muebleria.inventario.entidad.Venta;
import com.muebleria.inventario.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
    public ResponseEntity<?> guardar(@RequestBody Venta venta) {
        try {
            Venta ventaGuardada = ventaService.guardarConDetalle(venta);
            return ResponseEntity.ok(ventaGuardada);
        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable("id") Long id){
        ventaService.eliminar(id);
    }
}
