package com.muebleria.inventario.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialSimpleDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String descripcion;
    private String unidadDeMedida;
    private Long stockActual;

}
