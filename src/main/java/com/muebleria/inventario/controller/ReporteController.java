package com.muebleria.inventario.controller;

import com.muebleria.inventario.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/reporte")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/completo")
    public ResponseEntity<byte[]> descargarReporteCompleto() {
        try {
            byte[] reporte = reporteService.generarReporteCompleto();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("ReporteCompleto.xlsx")
                    .build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reporte);
        } catch (IOException e) {
            // Manejo simple de error, puedes mejorar el control
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
