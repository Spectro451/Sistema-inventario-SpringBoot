package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MaterialMuebleSimpleDTO;
import com.muebleria.inventario.dto.ProveedorMaterialSimpleDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.exception.ResourceNotFoundException;
import com.muebleria.inventario.repository.MaterialRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;




    public Optional<Material> buscarPorId(Long id) {
        return materialRepository.findById(id);
    }

    public Material guardar(Material material) {
        return materialRepository.save(material);
    }

    public void eliminar(Long id) {
        if (!materialRepository.existsById(id)) {
            throw new RuntimeException("Material con id " + id + " no existe.");
        }
        materialRepository.deleteById(id);
    }
    public List<MaterialDTO> mostrarTodosConRelacionesSimples() {
        List<Material> materiales = materialRepository.findAll();

        return materiales.stream().map(material -> {
            MaterialDTO dto = new MaterialDTO();
            dto.setId(material.getId());
            dto.setNombre(material.getNombre());
            dto.setTipo(material.getTipo().toString());
            dto.setDescripcion(material.getDescripcion());
            dto.setUnidadDeMedida(material.getUnidadDeMedida());
            dto.setStockActual(material.getStockActual());

            // Mapear ProveedorMaterial → ProveedorMaterialSimpleDTO
            List<ProveedorMaterialSimpleDTO> pmDTOS = material.getProveedorMateriales().stream()
                    .map(pm -> {
                        ProveedorMaterialSimpleDTO pms = new ProveedorMaterialSimpleDTO();
                        pms.setId(pm.getId());
                        pms.setCostoUnitario(pm.getCostoUnitario());
                        pms.setCantidadSuministrada(pm.getCantidadSuministrada());
                        // Nombre del proveedor extraído de la entidad Proveedor
                        pms.setNombreProveedor(pm.getProveedor().getNombre());
                        return pms;
                    })
                    .collect(Collectors.toList());
            dto.setProveedorMateriales(pmDTOS);

            // Mapear MaterialMueble → MaterialMuebleSimpleDTO
            List<MaterialMuebleSimpleDTO> mmDTOS = material.getMaterialMuebles().stream()
                    .map(mm -> {
                        MaterialMuebleSimpleDTO mms = new MaterialMuebleSimpleDTO();
                        mms.setId(mm.getId());
                        mms.setCantidadUtilizada(mm.getCantidadUtilizada());
                        // Nombre del mueble extraído de la entidad Mueble
                        mms.setNombreMueble(mm.getMueble().getNombre());
                        return mms;
                    })
                    .collect(Collectors.toList());
            dto.setMaterialMuebles(mmDTOS);

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Material actualizarMaterial(Long id, Material materialActualizado) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material no encontrado"));

        material.setNombre(materialActualizado.getNombre());
        material.setTipo(materialActualizado.getTipo());
        material.setDescripcion(materialActualizado.getDescripcion());
        material.setUnidadDeMedida(materialActualizado.getUnidadDeMedida());
        material.setStockActual(materialActualizado.getStockActual());

        return materialRepository.save(material);
    }
}
