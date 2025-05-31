package com.muebleria.inventario.repository;

import com.muebleria.inventario.entidad.Mueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MuebleRepository extends JpaRepository<Mueble, Long> {

}
