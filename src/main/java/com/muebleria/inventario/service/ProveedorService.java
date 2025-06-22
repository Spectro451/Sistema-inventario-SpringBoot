package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MaterialSimpleDTO;
import com.muebleria.inventario.dto.ProveedorDTO;
import com.muebleria.inventario.dto.ProveedorMaterialDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.entidad.TipoMaterial;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import com.muebleria.inventario.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    

    public Optional<Proveedor> getProveedoresId(Long id) {
        return proveedorRepository.findById(id);
    }

    @Transactional
    public Proveedor guardarProveedor(ProveedorDTO proveedorDTO) {

        // Mapeo básico ProveedorDTO -> Proveedor entidad
        Proveedor proveedor = new Proveedor();
        proveedor.setId(proveedorDTO.getId());
        proveedor.setNombre(proveedorDTO.getNombre());
        proveedor.setTelefono(proveedorDTO.getTelefono());
        proveedor.setCorreo(proveedorDTO.getCorreo());
        proveedor.setDireccion(proveedorDTO.getDireccion());
        proveedor.setFechaPedido(proveedorDTO.getFechaPedido());

        // Guardar proveedor primero para obtener ID
        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);

        List<ProveedorMateriales> pmGuardados = new ArrayList<>();

        if (proveedorDTO.getProveedorMateriales() != null) {

            for (ProveedorMaterialDTO pmDTO : proveedorDTO.getProveedorMateriales()) {

                MaterialSimpleDTO materialDTO = pmDTO.getMaterial();
                Material materialFinal;

                if (materialDTO.getId() != null) {
                    // Material ya existe, buscar y actualizar stock si es necesario
                    materialFinal = materialRepository.findById(materialDTO.getId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Material no encontrado con id: " + materialDTO.getId()
                            ));
                } else {
                    // Crear nuevo material
                    materialFinal = new Material();
                    materialFinal.setNombre(materialDTO.getNombre());

                    // Convertir String a TipoMaterial (enum)
                    try {
                        TipoMaterial tipoEnum = TipoMaterial.valueOf(materialDTO.getTipo().toUpperCase());
                        materialFinal.setTipo(tipoEnum);
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new RuntimeException("Tipo de material inválido o no especificado: " + materialDTO.getTipo());
                    }

                    materialFinal.setDescripcion(materialDTO.getDescripcion());
                    materialFinal.setUnidadDeMedida(materialDTO.getUnidadDeMedida());
                    materialFinal.setStockActual(materialDTO.getStockActual() != null ? materialDTO.getStockActual() : 0L);

                    materialFinal = materialRepository.save(materialFinal);
                }

                // Verificar si ya existe la relación ProveedorMateriales para evitar duplicados
                boolean existeRelacion = proveedorMaterialesRepository
                        .existsByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), materialFinal.getId());

                if (existeRelacion) {
                    ProveedorMateriales existente = proveedorMaterialesRepository
                            .findByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), materialFinal.getId());

                    Long cantidadAnterior = existente.getCantidadSuministrada() != null ? existente.getCantidadSuministrada() : 0L;
                    Long nuevaCantidad = pmDTO.getCantidadSuministrada() != null ? pmDTO.getCantidadSuministrada() : 0L;

                    // Actualizar cantidad suministrada y stock si hay diferencia
                    if (!cantidadAnterior.equals(nuevaCantidad)) {
                        existente.setCantidadSuministrada(nuevaCantidad);

                        Long diferencia = nuevaCantidad - cantidadAnterior;
                        Long nuevoStock = materialFinal.getStockActual() + diferencia;
                        materialFinal.setStockActual(nuevoStock);
                        materialRepository.save(materialFinal);

                        proveedorMaterialesRepository.save(existente);
                    }

                    pmGuardados.add(existente);
                    continue;
                }

                // Crear la relación proveedor-material con costo unitario y cantidad suministrada
                ProveedorMateriales pmNuevo = new ProveedorMateriales();
                pmNuevo.setProveedor(proveedorGuardado);
                pmNuevo.setMaterial(materialFinal);
                pmNuevo.setCostoUnitario(pmDTO.getCostoUnitario());
                pmNuevo.setCantidadSuministrada(pmDTO.getCantidadSuministrada() != null ? pmDTO.getCantidadSuministrada() : 0L);

                // Actualizar stock sumando cantidad suministrada nueva
                Long stockActual = materialFinal.getStockActual() != null ? materialFinal.getStockActual() : 0L;
                Long cantidadSum = pmNuevo.getCantidadSuministrada();
                materialFinal.setStockActual(stockActual + cantidadSum);
                materialRepository.save(materialFinal);

                ProveedorMateriales pmResult = proveedorMaterialesService.guardar(pmNuevo);
                pmGuardados.add(pmResult);
            }
        }

        // Actualizar la lista de relaciones en proveedor
        proveedorGuardado.getProveedorMateriales().clear();
        proveedorGuardado.getProveedorMateriales().addAll(pmGuardados);

        return proveedorGuardado;
    }

    public void eliminarProveedor(Long id) {
        if (!proveedorRepository.existsById(id)) {
            throw new RuntimeException("Proveedor con id " + id + " no existe.");
        }
        proveedorRepository.deleteById(id);
    }

    public List<ProveedorDTO> findAllDTO() {
        List<Proveedor> proveedores = proveedorRepository.findAll();

        return proveedores.stream().map(proveedor -> {
            ProveedorDTO proveedorDTO = new ProveedorDTO();
            proveedorDTO.setId(proveedor.getId());
            proveedorDTO.setNombre(proveedor.getNombre());
            proveedorDTO.setTelefono(proveedor.getTelefono());
            proveedorDTO.setCorreo(proveedor.getCorreo());
            proveedorDTO.setDireccion(proveedor.getDireccion());
            proveedorDTO.setFechaPedido(proveedor.getFechaPedido());

            if (proveedor.getProveedorMateriales() != null) {
                List<ProveedorMaterialDTO> pmDTOs = proveedor.getProveedorMateriales().stream().map(pm -> {
                    ProveedorMaterialDTO pmDTO = new ProveedorMaterialDTO();
                    pmDTO.setId(pm.getId());
                    pmDTO.setCostoUnitario(pm.getCostoUnitario());
                    pmDTO.setCantidadSuministrada(pm.getCantidadSuministrada());

                    Material material = pm.getMaterial();
                    MaterialSimpleDTO materialSimpleDTO = new MaterialSimpleDTO();
                    materialSimpleDTO.setId(material.getId());
                    materialSimpleDTO.setNombre(material.getNombre());
                    materialSimpleDTO.setTipo(material.getTipo().toString()); // Si usas enum
                    materialSimpleDTO.setDescripcion(material.getDescripcion());
                    materialSimpleDTO.setUnidadDeMedida(material.getUnidadDeMedida());
                    materialSimpleDTO.setStockActual(material.getStockActual());

                    pmDTO.setMaterial(materialSimpleDTO);

                    return pmDTO;
                }).collect(Collectors.toList());

                proveedorDTO.setProveedorMateriales(pmDTOs);
            } else {
                proveedorDTO.setProveedorMateriales(new ArrayList<>());
            }

            return proveedorDTO;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Proveedor actualizarProveedor(Long id, Proveedor proveedorActualizado) {
        Proveedor existente = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));

        // 1. Actualiza campos simples
        existente.setNombre(proveedorActualizado.getNombre());
        existente.setTelefono(proveedorActualizado.getTelefono());
        existente.setCorreo(proveedorActualizado.getCorreo());
        existente.setDireccion(proveedorActualizado.getDireccion());
        existente.setFechaPedido(proveedorActualizado.getFechaPedido());

        // 2. Si vienen relaciones a Materiales
        if (proveedorActualizado.getProveedorMateriales() != null) {
            Map<Long, ProveedorMateriales> mapExistentes = existente.getProveedorMateriales().stream()
                    .collect(Collectors.toMap(ProveedorMateriales::getId, Function.identity()));

            List<ProveedorMateriales> procesados = new ArrayList<>();

            for (ProveedorMateriales pmActualizado : proveedorActualizado.getProveedorMateriales()) {
                Material material = pmActualizado.getMaterial();

                if (pmActualizado.getId() != null && mapExistentes.containsKey(pmActualizado.getId())) {
                    // RELACIÓN EXISTENTE → ACTUALIZAR
                    ProveedorMateriales original = mapExistentes.remove(pmActualizado.getId());

                    Long viejaCantidad = original.getCantidadSuministrada() != null ? original.getCantidadSuministrada() : 0L;
                    Long nuevaCantidad = pmActualizado.getCantidadSuministrada() != null ? pmActualizado.getCantidadSuministrada() : 0L;
                    Long diferencia = nuevaCantidad - viejaCantidad;

                    original.setCostoUnitario(pmActualizado.getCostoUnitario());
                    original.setCantidadSuministrada(nuevaCantidad);

                    // Ajustar stock solo si hay diferencia
                    if (diferencia != 0) {
                        Long stockActual = original.getMaterial().getStockActual() != null ? original.getMaterial().getStockActual() : 0L;
                        original.getMaterial().setStockActual(stockActual + diferencia);
                        materialRepository.save(original.getMaterial());
                    }

                    procesados.add(proveedorMaterialesService.guardarOActualizar(original));

                } else {
                    // NUEVA RELACIÓN
                    pmActualizado.setProveedor(existente);

                    Long stockActual = pmActualizado.getMaterial().getStockActual() != null ? pmActualizado.getMaterial().getStockActual() : 0L;
                    Long cantidad = pmActualizado.getCantidadSuministrada() != null ? pmActualizado.getCantidadSuministrada() : 0L;

                    pmActualizado.getMaterial().setStockActual(stockActual + cantidad);
                    materialRepository.save(pmActualizado.getMaterial());

                    ProveedorMateriales creado = proveedorMaterialesService.guardarOActualizar(pmActualizado);
                    procesados.add(creado);
                }

// RELACIONES ELIMINADAS → RESTAR DEL STOCK Y ELIMINAR
                for (ProveedorMateriales eliminado : mapExistentes.values()) {
                    Long stockActual = eliminado.getMaterial().getStockActual() != null ? eliminado.getMaterial().getStockActual() : 0L;
                    Long cantidad = eliminado.getCantidadSuministrada() != null ? eliminado.getCantidadSuministrada() : 0L;

                    eliminado.getMaterial().setStockActual(stockActual - cantidad);
                    materialRepository.save(eliminado.getMaterial());

                    proveedorMaterialesRepository.delete(eliminado);
                }
            }
            existente.getProveedorMateriales().clear();
            existente.getProveedorMateriales().addAll(procesados);
        }

        return proveedorRepository.save(existente);
    }
}
