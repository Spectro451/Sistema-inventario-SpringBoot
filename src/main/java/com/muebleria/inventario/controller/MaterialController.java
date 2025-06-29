package com.muebleria.inventario.controller;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MuebleDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.TipoMaterial;
import com.muebleria.inventario.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/materiales")

public class MaterialController {

    @Autowired
    MaterialService materialService;

//    @GetMapping
//    public List<Material> findAll() {
//        return materialService.mostrarTodos();
//    }

    @PostMapping
    public Material guardar(@RequestBody Material material) {
        materialService.guardar(material);
        return material;
    }

    @GetMapping("/{id}")
    public Optional<Material> buscarId(@PathVariable("id") Long id) {
        return materialService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            materialService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @GetMapping
    public ResponseEntity<List<MaterialDTO>> listarMateriales() {
        List<MaterialDTO> listaDTO = materialService.mostrarTodosConRelacionesSimples();
        return ResponseEntity.ok(listaDTO);
    }
    @GetMapping("/tipos")
    public ResponseEntity<TipoMaterial[]> obtenerTiposMaterial() {
        return ResponseEntity.ok(TipoMaterial.values());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Material> actualizarMaterial(@PathVariable Long id, @RequestBody Material material) {
        Material actualizado = materialService.actualizarMaterial(id, material);
        return ResponseEntity.ok(actualizado);
    }
    @GetMapping("/reporte")
    public ResponseEntity<byte[]> generarReporteMaterial() throws IOException {
        byte[] excelBytes = materialService.generarReporteMaterial();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Materiales.xlsx").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
