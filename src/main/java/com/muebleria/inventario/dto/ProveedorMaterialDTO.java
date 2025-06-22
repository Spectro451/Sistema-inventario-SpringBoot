package com.muebleria.inventario.dto;

import lombok.Data;

@Data
public class ProveedorMaterialDTO {
    private Long id;
    private Long costoUnitario;
    private Long cantidadSuministrada;
    private MaterialSimpleDTO material;
}
