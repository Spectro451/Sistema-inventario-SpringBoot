package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MaterialMuebleSimpleDTO;
import com.muebleria.inventario.dto.ProveedorMaterialSimpleDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.exception.ResourceNotFoundException;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.util.ExcelStyleUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public byte[] generarReporteMaterial() throws IOException {
        List<MaterialDTO> materiales = mostrarTodosConRelacionesSimples();

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = ExcelStyleUtil.crearEstiloTitulo(workbook);
            CellStyle estiloDatos = ExcelStyleUtil.crearEstiloDatos(workbook);
            CellStyle estiloStockBajo = ExcelStyleUtil.crearEstiloStockBajo(workbook);
            Sheet hojaDatos = workbook.createSheet("MaterialDatos");

            // Encabezados
            Row header = hojaDatos.createRow(0);
            String[] columnas = {"ID", "Nombre", "Tipo", "Descripción", "Unidad", "Stock", "Nombre Proveedor", "Nombre Mueble"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue("   " + columnas[i] + "   ");
                cell.setCellStyle(estiloTitulo); // Aplico estilo al encabezado
            }

            int rowNum = 1;
            for (MaterialDTO material : materiales) {
                Row row = hojaDatos.createRow(rowNum++);
                row.createCell(0).setCellValue(material.getId());
                row.createCell(1).setCellValue(material.getNombre());
                row.createCell(2).setCellValue(material.getTipo().toString());
                row.createCell(3).setCellValue(material.getDescripcion());
                row.createCell(4).setCellValue(material.getUnidadDeMedida());
                row.createCell(5).setCellValue(material.getStockActual());

                // Concatenar nombres de proveedores
                String nombresProveedores = material.getProveedorMateriales().stream()
                        .map(pm -> pm.getNombreProveedor())
                        .distinct()
                        .collect(Collectors.joining(", "));

                // Concatenar nombres de muebles
                String nombresMuebles = material.getMaterialMuebles().stream()
                        .map(mm -> mm.getNombreMueble())
                        .distinct()
                        .collect(Collectors.joining(", "));

                row.createCell(6).setCellValue(nombresProveedores);
                row.createCell(7).setCellValue(nombresMuebles);


                // Aplico el estilo a todas las celdas
                for (int i = 0; i <= 7; i++) {
                    if (i == 5) { // columna de stock
                        Cell stockCell = row.getCell(i);
                        if (material.getStockActual() <= 10) {
                            stockCell.setCellStyle(estiloStockBajo);
                        } else {
                            stockCell.setCellStyle(estiloDatos);
                        }
                    } else {
                        row.getCell(i).setCellStyle(estiloDatos);
                    }
                }
            }
            hojaDatos.setAutoFilter(new CellRangeAddress(
                    0,
                    materiales.size(),
                    0,
                    7
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
}
