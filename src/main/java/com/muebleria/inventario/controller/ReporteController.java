package com.muebleria.inventario.controller;

import com.muebleria.inventario.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/reporte")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/material")
    public ResponseEntity<byte[]> generarReporteMaterial() throws IOException {
        byte[] excelBytes = reporteService.generarReporteMaterial();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Materiales.xlsx").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
