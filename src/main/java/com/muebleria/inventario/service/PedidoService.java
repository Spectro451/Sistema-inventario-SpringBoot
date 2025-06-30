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
        Long proveedorId = pedidoDTO.getProveedor().getId();
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + proveedorId));

        Pedido pedido = new Pedido();
        pedido.setFechaPedido(pedidoDTO.getFechaPedido());
        pedido.setProveedor(proveedor);

        long costoTotalPedido = 0L;
        long sumaCantidadPedido = 0L;

        List<ProveedorMaterialDTO> materiales = pedidoDTO.getProveedor().getProveedorMateriales();
        if (materiales != null && !materiales.isEmpty()) {
            for (ProveedorMaterialDTO pmDTO : materiales) {
                Long relacionId = pmDTO.getId();
                if (relacionId == null) {
                    throw new RuntimeException("Falta ID de la relación ProveedorMateriales");
                }

                ProveedorMateriales relacion = proveedorMaterialesRepository.findById(relacionId)
                        .orElseThrow(() -> new RuntimeException("Relación ProveedorMateriales no encontrada con id: " + relacionId));

                Material material = relacion.getMaterial();

                Long cantidadPedidoMaterial = pmDTO.getCantidadSuministrada() != null ? pmDTO.getCantidadSuministrada() : 0L;
                if (cantidadPedidoMaterial <= 0) continue;  // Ignorar cantidades 0 o negativas

                // Actualizar stock del material
                Long nuevoStock = (material.getStockActual() != null ? material.getStockActual() : 0L) + cantidadPedidoMaterial;
                material.setStockActual(nuevoStock);
                materialRepository.save(material);

                // Actualizar cantidad suministrada en relación proveedor-material
                Long cantidadAnterior = relacion.getCantidadSuministrada() != null ? relacion.getCantidadSuministrada() : 0L;
                relacion.setCantidadSuministrada(cantidadAnterior + cantidadPedidoMaterial);
                proveedorMaterialesRepository.save(relacion);

                // Acumular costos y cantidades para el pedido
                Long costoUnitario = relacion.getCostoUnitario() != null ? relacion.getCostoUnitario() : 0L;
                costoTotalPedido += costoUnitario * cantidadPedidoMaterial;
                sumaCantidadPedido += cantidadPedidoMaterial;
            }
        }

        // Guardar cantidadPedido y costoTotal calculados en pedido
        pedido.setCantidadPedido(sumaCantidadPedido);
        pedido.setCostoTotal(costoTotalPedido);

        Pedido pedidoGuardado = pedidoRepository.save(pedido);

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

        // 🔧 FALTABA ESTO:
        dto.setCantidadPedido(pedido.getCantidadPedido());
        dto.setCostoTotal(pedido.getCostoTotal());

        return dto;
    }
}
