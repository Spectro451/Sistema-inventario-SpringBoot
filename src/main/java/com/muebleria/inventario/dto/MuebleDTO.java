package com.muebleria.inventario.dto;

import com.muebleria.inventario.dto.MaterialMuebleDTO;
import lombok.Data;

import java.util.List;

@Data
public class MuebleDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long precioVenta;
    private Long stock;
    private List<MaterialMuebleDTO> materialMuebles;
}
