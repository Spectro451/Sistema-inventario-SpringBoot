package com.muebleria.inventario.controller;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MuebleDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
