package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "MaterialMueble",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"mueble_id", "material_id"})
        }
)
public class MaterialMueble {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mueble_id", nullable = false)
    @JsonBackReference
    private Mueble mueble;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "cantidadUtilizada", nullable = false)
    private Long cantidadUtilizada;
}
