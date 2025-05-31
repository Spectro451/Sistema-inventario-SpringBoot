package com.muebleria.inventario.entidad;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "VentaMueble",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"muebleId", "ventaId"})}
)
public class VentaMueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "muebleId", nullable = false)
    private Mueble mueble;

    @ManyToOne
    @JoinColumn(name = "ventaId", nullable = false)
    private Venta venta;

    @Column(name = "cantidad", nullable = false)
    private Long cantidad;

    @Column(name = "precioUnitario", nullable = false)
    private Long precioUnitario;

    @Column(name = "subtotal", nullable = false)
    private Long subtotal;
}
