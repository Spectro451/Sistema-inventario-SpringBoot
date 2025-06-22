package com.muebleria.inventario.dto;

import lombok.Data;

@Data
public class ProveedorMaterialSimpleDTO {
    private Long id;
    private Long costoUnitario;
    private Long cantidadSuministrada;
    private String nombreProveedor;
}
