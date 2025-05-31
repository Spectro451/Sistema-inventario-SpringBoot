package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "Mueble")
public class Mueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private Long precioVenta;

    @Column(nullable = false)
    private Long stock;

    @OneToMany(mappedBy = "mueble", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("mueble-materialMuebles")
    private List<MaterialMueble> materialMuebles = new ArrayList<>();

    @OneToMany(mappedBy = "mueble", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("mueble-ventaMuebles")
    private List<VentaMueble> ventaMuebles = new ArrayList<>();

}
