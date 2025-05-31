package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;

import java.util.List;

@Entity
@Data
@Table(name = "Venta")
public class Venta {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @Column(nullable = false)
    private Long total;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "venta-ventamueble")
    private List<VentaMueble> ventaMuebles = new ArrayList<>();
}
