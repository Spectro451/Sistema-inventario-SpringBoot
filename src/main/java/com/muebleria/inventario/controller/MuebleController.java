package com.muebleria.inventario.controller;

import com.muebleria.inventario.dto.MuebleDTO;
import com.muebleria.inventario.entidad.MaterialMueble;
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
@CrossOrigin(origins = "http://localhost:4200")
public class MuebleController {

    @Autowired
    MuebleService muebleService;

//    @GetMapping
//    public List<Mueble> getMuebles() {
//        return muebleService.findAll();
//    }

    @GetMapping("/{id}")
    public ResponseEntity<MuebleDTO> getByID(@PathVariable Long id) {
        MuebleDTO muebleDTO = muebleService.findById(id);
        return ResponseEntity.ok(muebleDTO);
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
    public ResponseEntity<?> updateMueble(@PathVariable Long id, @RequestBody Mueble dto) {
        System.out.println("➡️ Recibido mueble ID: " + dto.getId());
        System.out.println("➡️ Stock: " + dto.getStock());
        System.out.println("➡️ Materiales: ");
        for (MaterialMueble mm : dto.getMaterialMuebles()) {
            System.out.println("  🔹 ID MaterialMueble: " + mm.getId() +
                    ", Material ID: " + mm.getMaterial().getId() +
                    ", cantidad usada: " + mm.getCantidadUtilizada());
        }

        Mueble actualizado = muebleService.update(id, dto);
        return ResponseEntity.ok(actualizado);
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
    @GetMapping
    public List<MuebleDTO> listarMuebles() {

        return muebleService.findAllDTO();
    }
}
