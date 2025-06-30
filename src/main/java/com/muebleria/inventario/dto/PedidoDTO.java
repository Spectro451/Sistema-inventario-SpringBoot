package com.muebleria.inventario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PedidoDTO {
    private Long id;

    @NotNull(message = "La fecha de pedido es obligatoria")
    @JsonFormat(pattern = "dd/MM/yyyy")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate fechaPedido;

    private ProveedorDTO proveedor;
    private Long cantidadPedido;
    private Long costoTotal;
}
