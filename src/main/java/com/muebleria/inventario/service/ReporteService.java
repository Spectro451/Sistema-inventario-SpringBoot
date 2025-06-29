package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.repository.MaterialRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ReporteService {
    @Autowired
    private MaterialRepository materialRepository;

    public class ExcelStyleUtil {

        public static CellStyle crearEstiloTitulo(Workbook workbook) {
            CellStyle estilo = workbook.createCellStyle();

            // Fondo color (por ejemplo azul oscuro)
            estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fuente en negrita, color blanco
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            estilo.setFont(font);

            // Centrar texto horizontal y vertical
            estilo.setAlignment(HorizontalAlignment.CENTER);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);

            // Bordes en todas las celdas
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);

            return estilo;
        }

        public static CellStyle crearEstiloDatos(Workbook workbook) {
            CellStyle estilo = workbook.createCellStyle();

            // Fondo color claro (por ejemplo gris claro)
            estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fuente normal, color negro
            Font font = workbook.createFont();
            font.setColor(IndexedColors.BLACK.getIndex());
            font.setFontHeightInPoints((short) 13);
            estilo.setFont(font);

            // Centrar texto horizontal y vertical
            estilo.setAlignment(HorizontalAlignment.CENTER);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);

            // Bordes en todas las celdas
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);

            // Para que el texto ajuste el contenido si es largo
            estilo.setWrapText(false);

            return estilo;
        }
        public static CellStyle crearEstiloStockBajo(Workbook workbook) {
            CellStyle estilo = crearEstiloDatos(workbook); // parte del estilo base
            CellStyle nuevoEstilo = workbook.createCellStyle();
            nuevoEstilo.cloneStyleFrom(estilo);

            nuevoEstilo.setFillForegroundColor(IndexedColors.RED.getIndex());
            nuevoEstilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            return nuevoEstilo;
        }
    }

    public byte[] generarReporteMaterial() throws IOException {
        List<Material> materiales = materialRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = ExcelStyleUtil.crearEstiloTitulo(workbook);
            CellStyle estiloDatos = ExcelStyleUtil.crearEstiloDatos(workbook);
            CellStyle estiloStockBajo = ExcelStyleUtil.crearEstiloStockBajo(workbook);
            Sheet hojaDatos = workbook.createSheet("MaterialDatos");

            // Encabezados
            Row header = hojaDatos.createRow(0);
            String[] columnas = {"ID", "Nombre", "Tipo", "Descripción", "Unidad", "Stock"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(" " + columnas[i] + " ");
                cell.setCellStyle(estiloTitulo); // Aplico estilo al encabezado
            }

            int rowNum = 1;
            for (Material material : materiales) {
                Row row = hojaDatos.createRow(rowNum++);
                row.createCell(0).setCellValue(material.getId());
                row.createCell(1).setCellValue(material.getNombre());
                row.createCell(2).setCellValue(material.getTipo().toString());
                row.createCell(3).setCellValue(material.getDescripcion());
                row.createCell(4).setCellValue(material.getUnidadDeMedida());
                row.createCell(5).setCellValue(material.getStockActual());

                // Aplico el estilo a todas las celdas
                for (int i = 0; i <= 5; i++) {
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
                    0,                      // Fila encabezado
                    materiales.size(),      // Última fila con datos
                    0,                      // Primera columna (ID)
                    5                       // Última columna (Stock)
            ));

            // Autoajustar ancho columnas
            for (int i = 0; i < columnas.length; i++) {
                hojaDatos.autoSizeColumn(i);
                int currentWidth = hojaDatos.getColumnWidth(i);
                int extraWidth = 1000;  // unidades POI, ajusta este valor a tu gusto
                hojaDatos.setColumnWidth(i, currentWidth + extraWidth);
            }

            // Hoja gráfica vacía por ahora
            workbook.createSheet("MaterialGrafico");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
