package com.muebleria.inventario.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class VentaDTO {
    private Long id;
    private LocalDate fecha;
    private Long total;
    private List<VentaMuebleDTO> ventaMuebles;
}
