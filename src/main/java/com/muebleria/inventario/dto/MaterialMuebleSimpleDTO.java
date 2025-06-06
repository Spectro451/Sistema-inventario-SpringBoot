package com.muebleria.inventario.dto;

import lombok.Data;

// MaterialMuebleDTO.java
@Data
public class MaterialMuebleSimpleDTO {
    private Long id;
    private Long cantidadUtilizada;
    private String nombreMueble;
}
