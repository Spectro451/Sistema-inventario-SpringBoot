package com.muebleria.inventario.dto;

import lombok.Data;

import java.util.List;

@Data
public class VentaMuebleDTO {
    private Long id;
    private Long cantidad;
    private Long precioUnitario;
    private Long subtotal;
    private String nombreMueble;
}
