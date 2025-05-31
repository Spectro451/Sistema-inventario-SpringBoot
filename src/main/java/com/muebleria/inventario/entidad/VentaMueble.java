package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JoinColumn(name = "mueble_id", nullable = false)
    @JsonBackReference("mueble-ventaMuebles")
    private Mueble mueble;

    @ManyToOne
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonBackReference(value = "venta-ventamueble")
    private Venta venta;

    @Column(name = "cantidad", nullable = false)
    private Long cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private Long precioUnitario;

    @Column(name = "subtotal", nullable = false)
    private Long subtotal;
}
