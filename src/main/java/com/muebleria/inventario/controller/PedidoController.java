package com.muebleria.inventario.controller;

import com.muebleria.inventario.dto.PedidoDTO;
import com.muebleria.inventario.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedido")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoDTO> crearPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        PedidoDTO nuevoPedido = pedidoService.guardar(pedidoDTO);
        return ResponseEntity.ok(nuevoPedido);
    }

    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody PedidoDTO pedidoDTO) {
        pedidoDTO.setId(id);
        return ResponseEntity.ok(pedidoService.actualizar(pedidoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/reporte")
    public ResponseEntity<byte[]> generarReportePedido() throws IOException {
        byte[] excelBytes = pedidoService.generarReportePedido();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Materiales.xlsx").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
