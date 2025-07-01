package com.muebleria.inventario.util;

import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.annotation.Bean;


public class ExcelStyleUtil {

    public static CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(font);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        return estilo;
    }

    public static CellStyle crearEstiloDatos(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setFontHeightInPoints((short) 13);
        estilo.setFont(font);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setWrapText(false);
        return estilo;
    }

    public static CellStyle crearEstiloStockBajo(Workbook workbook) {
        CellStyle estilo = crearEstiloDatos(workbook);
        estilo.setFillForegroundColor(IndexedColors.RED.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }
    public static CellStyle crearEstiloFecha(Workbook workbook) {
        CellStyle estilo = crearEstiloDatos(workbook);

        CreationHelper createHelper = workbook.getCreationHelper();
        estilo.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

        return estilo;
    }
    public static CellStyle crearEstiloMoneda(Workbook workbook) {
        CellStyle estilo = crearEstiloDatos(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();

        estilo.setDataFormat(createHelper.createDataFormat().getFormat("$#,##0"));
        return estilo;
    }
    public static CellStyle crearEstiloWrap(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();            // crear estilo vacío
        estilo.cloneStyleFrom(crearEstiloDatos(workbook));        // clonar estilo datos
        estilo.setWrapText(true);                                 // activar wrap text
        return estilo;
    }
}
