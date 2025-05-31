package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "ProveedorMateriales",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"proveedor_id", "material_id"})
    }
)
public class ProveedorMateriales {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", nullable = false)
    @JsonBackReference("proveedor-materiales")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    @JsonBackReference("material-proveedorMateriales")
    private Material material;

    @Column(name = "costo_unitario", nullable = false)
    private Long costoUnitario;
}
