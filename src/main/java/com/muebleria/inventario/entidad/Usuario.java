package com.muebleria.inventario.entidad;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "Usuario",
uniqueConstraints = {@UniqueConstraint(columnNames = "nombre")}
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;
}
