package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import com.muebleria.inventario.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {
    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProveedorMaterialesRepository proveedorMaterialesRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ProveedorMaterialesService proveedorMaterialesService;

    public List<Proveedor> getProveedores() {
        return proveedorRepository.findAll();
    }

    public Optional<Proveedor> getProveedoresId(Long id) {
        return proveedorRepository.findById(id);
    }

    @Transactional
    public Proveedor guardarProveedor(Proveedor proveedor) {

        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);

        if (proveedor.getProveedorMateriales() != null) {
            List<ProveedorMateriales> pmGuardados = new ArrayList<>();

            for (ProveedorMateriales pmOriginal : proveedor.getProveedorMateriales()) {
                Long materialId = pmOriginal.getMaterial().getId();


                Material material = materialRepository.findById(materialId)
                        .orElseThrow(() -> new RuntimeException(
                                "Material no encontrado con id: " + materialId
                        ));

                boolean yaExiste = proveedorMaterialesRepository
                        .existsByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), materialId);

                if (yaExiste) {

                    ProveedorMateriales pmExistente = proveedorMaterialesRepository
                            .findByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), materialId);

                    pmGuardados.add(pmExistente);
                    continue;
                }

                ProveedorMateriales pmAux = new ProveedorMateriales();
                pmAux.setProveedor(proveedorGuardado);
                pmAux.setMaterial(material);
                pmAux.setCostoUnitario(pmOriginal.getCostoUnitario());

                ProveedorMateriales pmResult = proveedorMaterialesService.guardar(pmAux);

                pmGuardados.add(pmResult);
            }

            proveedorGuardado.getProveedorMateriales().clear();


            proveedorGuardado.getProveedorMateriales().addAll(pmGuardados);
        }

        return proveedorGuardado;
    }

    public void eliminarProveedor(Long id) {
        if (!proveedorRepository.existsById(id)) {
            throw new RuntimeException("Proveedor con id " + id + " no existe.");
        }
        proveedorRepository.deleteById(id);
    }
}
