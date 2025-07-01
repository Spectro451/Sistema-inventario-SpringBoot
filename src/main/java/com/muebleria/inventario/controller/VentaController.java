package com.muebleria.inventario.controller;

import com.muebleria.inventario.dto.VentaDTO;
import com.muebleria.inventario.entidad.Venta;
import com.muebleria.inventario.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1/venta")
public class VentaController {

    @Autowired
    VentaService ventaService;

//    @GetMapping
//    public List<Venta> listar() {
//        return ventaService.findAll();
//    }

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
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            ventaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping
    public List<VentaDTO> listarVentas() {
        return ventaService.findAllDTO();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Venta> actualizarVenta(@PathVariable Long id, @RequestBody Venta venta) {
        try {
            Venta ventaActualizada = ventaService.update(id, venta);
            return ResponseEntity.ok(ventaActualizada);
        } catch (RuntimeException e) {
            // Aquí puedes manejar errores específicos y devolver diferentes códigos HTTP si quieres
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }
    @GetMapping("/reporte")
    public ResponseEntity<byte[]> generarReporteVenta() throws IOException {
        byte[] excelBytes = ventaService.generarReporteVenta();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Materiales.xlsx").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
