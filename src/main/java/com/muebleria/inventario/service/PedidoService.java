package com.muebleria.inventario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muebleria.inventario.dto.*;
import com.muebleria.inventario.entidad.*;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.PedidoRepository;
import com.muebleria.inventario.repository.ProveedorMaterialesRepository;
import com.muebleria.inventario.repository.ProveedorRepository;
import com.muebleria.inventario.util.ExcelStyleUtil;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
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
    private ObjectMapper objectMapper;

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

        List<DetallePedido> detalles = new ArrayList<>();

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
                if (cantidadPedidoMaterial <= 0) continue;

                Long nuevoStock = (material.getStockActual() != null ? material.getStockActual() : 0L) + cantidadPedidoMaterial;
                material.setStockActual(nuevoStock);
                materialRepository.save(material);

                Long cantidadAnterior = relacion.getCantidadSuministrada() != null ? relacion.getCantidadSuministrada() : 0L;
                relacion.setCantidadSuministrada(cantidadAnterior + cantidadPedidoMaterial);
                proveedorMaterialesRepository.save(relacion);

                Long costoUnitario = relacion.getCostoUnitario() != null ? relacion.getCostoUnitario() : 0L;
                costoTotalPedido += costoUnitario * cantidadPedidoMaterial;
                sumaCantidadPedido += cantidadPedidoMaterial;

                // 👇 Agregar detalle del pedido
                DetallePedido detalle = new DetallePedido();
                detalle.setNombreMaterial(material.getNombre());
                detalle.setCantidad(cantidadPedidoMaterial);
                detalles.add(detalle);
            }
        }

        pedido.setCantidadPedido(sumaCantidadPedido);
        pedido.setCostoTotal(costoTotalPedido);
        pedido.setDetalleCantidades(detalles); // 👈 Agregar detalles al pedido

        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        PedidoDTO dto = toDTO(pedidoGuardado);
        dto.setDetalleCantidades(detalles); // 👈 También en el DTO

        return dto;
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


        dto.setCantidadPedido(pedido.getCantidadPedido());
        dto.setCostoTotal(pedido.getCostoTotal());
        dto.setDetalleCantidades(pedido.getDetalleCantidades());

        return dto;
    }

    public byte[] generarReportePedido() throws IOException {
        List<PedidoDTO> pedidos = listarTodos();

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = ExcelStyleUtil.crearEstiloTitulo(workbook);
            CellStyle estiloDatos = ExcelStyleUtil.crearEstiloDatos(workbook);
            CellStyle estiloFecha = ExcelStyleUtil.crearEstiloFecha(workbook);
            CellStyle estiloMoneda = ExcelStyleUtil.crearEstiloMoneda(workbook);

            // Crear estilo con wrapText para materiales con saltos de línea
            CellStyle estiloWrap = workbook.createCellStyle();
            estiloWrap.cloneStyleFrom(estiloDatos);
            estiloWrap.setWrapText(true);

            Sheet hojaDatos = workbook.createSheet("Reporte Pedido");

            // Encabezados
            Row header = hojaDatos.createRow(0);
            String[] columnas = {"ID", "Fecha Pedido", "Cantidad Total", "Costo Total", "Proveedor", "Materiales Pedidos"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue("   " + columnas[i] + "   ");
                cell.setCellStyle(estiloTitulo);
            }

            int rowNum = 1;
            for (PedidoDTO pedido : pedidos) {
                Row row = hojaDatos.createRow(rowNum++);
                row.createCell(0).setCellValue(pedido.getId());
                row.createCell(1).setCellValue(pedido.getFechaPedido());
                row.createCell(2).setCellValue(pedido.getCostoTotal());
                row.createCell(3).setCellValue(pedido.getCantidadPedido());
                row.createCell(4).setCellValue(pedido.getProveedor().getNombre());

                String nombresMaterial = "";
                if (pedido.getDetalleCantidades() != null) {
                    nombresMaterial = pedido.getDetalleCantidades().stream()
                            .map(mm -> String.format("%s: %d unidades ($%d c/u)",
                                    mm.getNombreMaterial(),
                                    mm.getCantidad(),
                                    buscarCostoUnitario(pedido.getProveedor().getProveedorMateriales(), mm.getNombreMaterial())
                            ))
                            .distinct()
                            .collect(Collectors.joining("\n"));
                }

                Cell materialesCell = row.createCell(5);
                materialesCell.setCellValue(nombresMaterial);
                materialesCell.setCellStyle(estiloWrap);

                for (int i = 0; i <= 5; i++) {
                    Cell c = row.getCell(i);
                    if (i == 1) {
                        c.setCellStyle(estiloFecha); // fecha con formato y estilo general
                    } else if (i == 5) {
                        c.setCellStyle(estiloWrap); // texto con wrap
                    } else if (i == 2) {
                        c.setCellStyle(estiloMoneda);
                    } else {
                        c.setCellStyle(estiloDatos); // resto celdas
                    }
                }
            }
            int ultimaColumna = columnas.length - 1;
            hojaDatos.setAutoFilter(new CellRangeAddress(
                    0,
                    pedidos.size(),
                    0,
                    ultimaColumna
            ));

            // Autoajustar ancho columnas
            for (int i = 0; i < columnas.length; i++) {
                hojaDatos.autoSizeColumn(i);
                int currentWidth = hojaDatos.getColumnWidth(i);
                int extraWidth = 738;
                hojaDatos.setColumnWidth(i, currentWidth + extraWidth);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    private Long buscarCostoUnitario(List<ProveedorMaterialDTO> proveedorMateriales, String nombreMaterial) {
        if (proveedorMateriales == null) return 0L;

        for (ProveedorMaterialDTO pm : proveedorMateriales) {
            if (pm.getMaterial() != null && nombreMaterial.equals(pm.getMaterial().getNombre())) {
                return pm.getCostoUnitario() != null ? pm.getCostoUnitario() : 0L;
            }
        }
        return 0L;
    }
}
