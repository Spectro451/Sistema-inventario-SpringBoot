package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.MuebleRepository;
import com.muebleria.inventario.util.ExcelStyleUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReporteService {
    @Autowired
    private MaterialService materialService;
    @Autowired
    MuebleService muebleService;


    public byte[] generarReporteCompleto() throws IOException {

        Map<String, byte[]> archivos = new HashMap<>();
        archivos.put("Material", materialService.generarReporteMaterial());
        archivos.put("Mueble", muebleService.generarReporteMueble());


        try (Workbook libroFinal = new XSSFWorkbook()) {

            for (Map.Entry<String, byte[]> entrada : archivos.entrySet()) {
                String nombreArchivo = entrada.getKey();
                byte[] contenido = entrada.getValue();

                try (InputStream is = new ByteArrayInputStream(contenido);
                     Workbook libroParcial = WorkbookFactory.create(is)) {

                    int cantidadHojas = libroParcial.getNumberOfSheets();

                    for (int i = 0; i < cantidadHojas; i++) {
                        Sheet hojaOrigen = libroParcial.getSheetAt(i);
                        String nombreHoja = nombreArchivo;

                        Sheet hojaDestino = libroFinal.createSheet(nombreHoja);

                        copiarHoja(hojaOrigen, hojaDestino);
                        int lastRow = hojaDestino.getLastRowNum();
                        if (lastRow >= 0) {
                            Row primeraFila = hojaDestino.getRow(0);
                            if (primeraFila != null) {
                                int lastCol = primeraFila.getLastCellNum() - 1;
                                hojaDestino.setAutoFilter(new CellRangeAddress(
                                        0,
                                        lastRow,
                                        0,
                                        lastCol
                                ));
                            }
                        }
                    }
                }
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libroFinal.write(salida);
            return salida.toByteArray();
        }
    }

    private void copiarHoja(Sheet origen, Sheet destino) {
        Workbook libroDestino = destino.getWorkbook();

        for (Row filaOrigen : origen) {
            Row filaDestino = destino.createRow(filaOrigen.getRowNum());

            for (Cell celdaOrigen : filaOrigen) {
                Cell celdaDestino = filaDestino.createCell(celdaOrigen.getColumnIndex());

                switch (celdaOrigen.getCellType()) {
                    case STRING -> celdaDestino.setCellValue(celdaOrigen.getStringCellValue());
                    case NUMERIC -> celdaDestino.setCellValue(celdaOrigen.getNumericCellValue());
                    case BOOLEAN -> celdaDestino.setCellValue(celdaOrigen.getBooleanCellValue());
                    case FORMULA -> celdaDestino.setCellFormula(celdaOrigen.getCellFormula());
                    case BLANK -> celdaDestino.setBlank();
                    default -> {}
                }

                if (celdaOrigen.getCellStyle() != null) {
                    CellStyle nuevoEstilo = libroDestino.createCellStyle();
                    nuevoEstilo.cloneStyleFrom(celdaOrigen.getCellStyle());
                    celdaDestino.setCellStyle(nuevoEstilo);
                }
            }
        }

        Row primeraFila = origen.getRow(0);
        if (primeraFila != null) {
            int ultimaCol = primeraFila.getLastCellNum();
            for (int i = 0; i < ultimaCol; i++) {
                destino.setColumnWidth(i, origen.getColumnWidth(i));
            }
        }
    }
}
