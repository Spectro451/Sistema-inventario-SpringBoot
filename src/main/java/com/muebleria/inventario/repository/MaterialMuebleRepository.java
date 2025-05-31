package com.muebleria.inventario.repository;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.MaterialMueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialMuebleRepository extends JpaRepository<MaterialMueble, Long> {

    List<MaterialMueble> findByMuebleId(Long muebleId);


    List<MaterialMueble> findByMaterialId(Long materialId);

    Long material(Material material);
}
