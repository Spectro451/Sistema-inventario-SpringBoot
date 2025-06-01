package com.muebleria.inventario.repository;

import com.muebleria.inventario.entidad.ProveedorMateriales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProveedorMaterialesRepository extends JpaRepository<ProveedorMateriales, Long> {

        List<ProveedorMateriales> findByProveedorId(Long proveedorId);

        List<ProveedorMateriales> findByMaterialId(Long materialId);

        boolean existsByProveedor_IdAndMaterial_Id(Long proveedorId, Long materialId);

        // <-- Agregar este método para obtener la tupla exacta
        ProveedorMateriales findByProveedor_IdAndMaterial_Id(Long proveedorId, Long materialId);
}

