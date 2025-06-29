package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.MaterialSimpleDTO;
import com.muebleria.inventario.dto.PedidoDTO;
import com.muebleria.inventario.dto.ProveedorDTO;
import com.muebleria.inventario.dto.ProveedorMaterialDTO;
import com.muebleria.inventario.entidad.*;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.PedidoRepository;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import com.muebleria.inventario.repository.ProveedorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ProveedorMaterialesRepository proveedorMaterialesRepository;

    @Autowired
    private ProveedorMaterialesService proveedorMaterialesService;

    @Transactional
    public PedidoDTO guardar(PedidoDTO pedidoDTO) {
        // 1. Obtener proveedor existente
        Long proveedorId = pedidoDTO.getProveedor().getId();
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + proveedorId));

        // 2. Crear nuevo pedido
        Pedido pedido = new Pedido();
        pedido.setFechaPedido(pedidoDTO.getFechaPedido());
        pedido.setProveedor(proveedor);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 3. Procesar materiales asociados (si hay)
        List<ProveedorMaterialDTO> materiales = pedidoDTO.getProveedor().getProveedorMateriales();
        if (materiales != null && !materiales.isEmpty()) {

            for (ProveedorMaterialDTO pmDTO : materiales) {

                MaterialSimpleDTO materialDTO = pmDTO.getMaterial();
                Material materialFinal;

                // Buscar o crear material
                if (materialDTO.getId() != null) {
                    materialFinal = materialRepository.findById(materialDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Material no encontrado con id: " + materialDTO.getId()));
                } else {
                    Optional<Material> materialExistente = materialRepository.findByNombreAndTipo(
                            materialDTO.getNombre(),
                            TipoMaterial.valueOf(materialDTO.getTipo().toUpperCase())
                    );
                    materialFinal = materialExistente.orElseGet(() -> {
                        Material nuevo = new Material();
                        nuevo.setNombre(materialDTO.getNombre());
                        nuevo.setTipo(TipoMaterial.valueOf(materialDTO.getTipo().toUpperCase()));
                        nuevo.setDescripcion(materialDTO.getDescripcion());
                        nuevo.setUnidadDeMedida(materialDTO.getUnidadDeMedida());
                        nuevo.setStockActual(materialDTO.getStockActual() != null ? materialDTO.getStockActual() : 0L);
                        return materialRepository.save(nuevo);
                    });
                }

                // Buscar relación existente
                ProveedorMateriales existente = proveedorMaterialesRepository
                        .findByProveedor_IdAndMaterial_Id(proveedor.getId(), materialFinal.getId());

                if (existente != null) {
                    // Sumar cantidades
                    Long cantidadAnterior = existente.getCantidadSuministrada() != null ? existente.getCantidadSuministrada() : 0L;
                    Long nuevaCantidad = pmDTO.getCantidadSuministrada() != null ? pmDTO.getCantidadSuministrada() : 0L;

                    existente.setCantidadSuministrada(cantidadAnterior + nuevaCantidad);
                    materialFinal.setStockActual(materialFinal.getStockActual() + nuevaCantidad);
                    materialRepository.save(materialFinal);
                    proveedorMaterialesRepository.save(existente);
                } else {
                    // Crear nueva relación
                    ProveedorMateriales nuevaRelacion = new ProveedorMateriales();
                    nuevaRelacion.setProveedor(proveedor);
                    nuevaRelacion.setMaterial(materialFinal);
                    nuevaRelacion.setCostoUnitario(pmDTO.getCostoUnitario());
                    nuevaRelacion.setCantidadSuministrada(pmDTO.getCantidadSuministrada() != null ? pmDTO.getCantidadSuministrada() : 0L);

                    // Aumentar stock
                    materialFinal.setStockActual(materialFinal.getStockActual() + nuevaRelacion.getCantidadSuministrada());
                    materialRepository.save(materialFinal);
                    proveedorMaterialesService.guardar(nuevaRelacion);
                }
            }
        }

        // 4. Devolver el DTO
        return toDTO(pedidoGuardado);
    }

    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PedidoDTO obtenerPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        return toDTO(pedido);
    }

    @Transactional
    public PedidoDTO actualizar(PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoRepository.findById(pedidoDTO.getId())
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setFechaPedido(pedidoDTO.getFechaPedido());
        return toDTO(pedidoRepository.save(pedido));
    }

    public void eliminar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new RuntimeException("No existe el pedido con ID " + id);
        }
        pedidoRepository.deleteById(id);
    }

    private PedidoDTO toDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setFechaPedido(pedido.getFechaPedido());

        Proveedor proveedor = pedido.getProveedor();
        if (proveedor != null) {
            ProveedorDTO pDto = new ProveedorDTO();
            pDto.setId(proveedor.getId());
            pDto.setNombre(proveedor.getNombre());
            pDto.setTelefono(proveedor.getTelefono());
            pDto.setCorreo(proveedor.getCorreo());
            pDto.setDireccion(proveedor.getDireccion());

            if (proveedor.getProveedorMateriales() != null) {
                List<ProveedorMaterialDTO> materialesDTO = proveedor.getProveedorMateriales().stream().map(pm -> {
                    ProveedorMaterialDTO pmDTO = new ProveedorMaterialDTO();
                    pmDTO.setId(pm.getId());
                    pmDTO.setCostoUnitario(pm.getCostoUnitario());
                    pmDTO.setCantidadSuministrada(pm.getCantidadSuministrada());

                    Material mat = pm.getMaterial();
                    MaterialSimpleDTO mDto = new MaterialSimpleDTO();
                    mDto.setId(mat.getId());
                    mDto.setNombre(mat.getNombre());
                    mDto.setTipo(mat.getTipo().toString());
                    mDto.setDescripcion(mat.getDescripcion());
                    mDto.setUnidadDeMedida(mat.getUnidadDeMedida());
                    mDto.setStockActual(mat.getStockActual());

                    pmDTO.setMaterial(mDto);
                    return pmDTO;
                }).collect(Collectors.toList());
                pDto.setProveedorMateriales(materialesDTO);
            } else {
                pDto.setProveedorMateriales(new ArrayList<>());
            }

            dto.setProveedor(pDto);
        }

        return dto;
    }
}
