package com.muebleria.inventario.dto;

import lombok.Data;

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
