package com.muebleria.inventario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProveedorDTO {
    private Long id;
    private String nombre;
    private String telefono;
    private String correo;
    private String direccion;

    private List<ProveedorMaterialDTO> proveedorMateriales;
}
