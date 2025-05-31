package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "Material")
public class Material {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMaterial tipo;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String unidadMedida;

    @Column(nullable = false)
    private Long stockActual;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("material-proveedorMateriales")
    private List<ProveedorMateriales> proveedorMateriales = new ArrayList<>();

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("material-materialMuebles")
    private List<MaterialMueble> materialMuebles = new ArrayList<>();
}
