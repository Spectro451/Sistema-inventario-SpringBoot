package com.muebleria.inventario.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String descripcion;
    private String unidadDeMedida;
    private Long stockActual;

    private List<ProveedorMaterialSimpleDTO> proveedorMateriales;
    private List<MaterialMuebleSimpleDTO> materialMuebles;
}
