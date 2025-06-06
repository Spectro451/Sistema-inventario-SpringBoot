package com.muebleria.inventario.dto;

import lombok.Data;

// MaterialMuebleDTO.java
@Data
public class MaterialMuebleDTO {
    private Long id;
    private Long cantidadUtilizada;
    private MaterialSimpleDTO material;
}
