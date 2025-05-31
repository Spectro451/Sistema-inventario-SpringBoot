package com.muebleria.inventario.repository;

import com.muebleria.inventario.entidad.VentaMueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VentaMuebleRepository extends JpaRepository<VentaMueble, Long> {

    List<VentaMueble> findByMuebleId(Long muebleId);


    List<VentaMueble> findByVentaId(Long ventaId);
}
