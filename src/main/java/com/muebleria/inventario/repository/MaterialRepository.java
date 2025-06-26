package com.muebleria.inventario.repository;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.TipoMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByNombreAndTipo(String nombre, TipoMaterial tipo);
}
