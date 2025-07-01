package com.muebleria.inventario.entidad;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class DetallePedido {
    private String nombreMaterial;
    private Long cantidad;
}
