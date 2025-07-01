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
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieSer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReporteService {
    @Autowired
    private MaterialService materialService;
    @Autowired
    MuebleService muebleService;
    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private ProveedorService proveedorService;
    @Autowired
    private VentaService ventaService;


    public byte[] generarReporteCompleto() throws IOException {

        Map<String, byte[]> archivos = new LinkedHashMap<>();
        archivos.put("Material", materialService.generarReporteMaterial());
        archivos.put("Mueble", muebleService.generarReporteMueble());
        archivos.put("Proveedores", proveedorService.generarReporteProveedor());
        archivos.put("Pedidos", pedidoService.generarReportePedido());
        archivos.put("Ventas", ventaService.generarReporteVenta());



        try (Workbook libroFinal = new XSSFWorkbook()) {

            for (Map.Entry<String, byte[]> entrada : archivos.entrySet()) {
                String nombreArchivo = entrada.getKey();
                byte[] contenido = entrada.getValue();

                try (InputStream is = new ByteArrayInputStream(contenido);
                     Workbook libroParcial = WorkbookFactory.create(is)) {

                    int cantidadHojas = libroParcial.getNumberOfSheets();

                    for (int i = 0; i < cantidadHojas; i++) {
                        Sheet hojaOrigen = libroParcial.getSheetAt(i);
                        String nombreHoja = hojaOrigen.getSheetName();
                        Sheet hojaDestino = libroFinal.createSheet(nombreHoja);

                        copiarHoja(hojaOrigen, hojaDestino);


                        if (libroParcial instanceof XSSFWorkbook partial && libroFinal instanceof XSSFWorkbook finalLibro) {
                            SheetVisibility visibilidad = partial.getSheetVisibility(i);
                            finalLibro.setSheetVisibility(
                                    finalLibro.getSheetIndex(hojaDestino),
                                    visibilidad
                            );
                        }

                        int lastRow = hojaDestino.getLastRowNum();
                        if (lastRow >= 0) {
                            Row primeraFila = hojaDestino.getRow(0);
                            if (primeraFila != null) {
                                int lastCol = primeraFila.getLastCellNum() - 1;
                                hojaDestino.setAutoFilter(new CellRangeAddress(0, lastRow, 0, lastCol));
                            }
                        }
                    }
                }
            }
            if (libroFinal instanceof XSSFWorkbook xssfLibro) {
                agregarGraficoVentas(xssfLibro);
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
    public void agregarGraficoVentas(XSSFWorkbook workbook) {
        XSSFSheet hojaAux = workbook.getSheet("aux_graf");
        if (hojaAux == null) return;

        XSSFSheet hojaGrafico = workbook.createSheet("Graficos Ventas");
        XSSFDrawing drawing = hojaGrafico.createDrawingPatriarch();

        // Cantidad de filas de cada bloque
        int filas1 = contarFilasConDatos(hojaAux, 1);
        int filas2 = contarFilasConDatos(hojaAux, 1 + filas1 + 2);
        int filas3 = contarFilasConDatos(hojaAux, 1 + filas1 + filas2 + 4);
        int filas4 = contarFilasConDatos(hojaAux, 1 + filas1 + filas2 + filas3 + 6);

        // Parámetros de layout
        int chartWidthCols   = 8;   // ancho de cada gráfico en columnas
        int chartHeightRows  = 15;  // altura en filas
        int gapCols          = 2;   // espacio horizontal entre gráficos
        int gapRows          = 3;   // espacio vertical

        // Coordenadas base
        int baseColLeft   = 1;
        int baseColRight  = baseColLeft + chartWidthCols + gapCols;
        int baseRowTop    = 1;
        int baseRowBottom = baseRowTop + chartHeightRows + gapRows;

        // 1) Gráfico 1 (fila superior, columna izquierda)
        crearGraficoBarras(drawing, hojaAux,
                1, filas1,
                "Mueble", "Cantidad Vendida", "Ventas por Mueble",
                baseColLeft, baseRowTop,
                baseColLeft + chartWidthCols, baseRowTop + chartHeightRows
        );

        // 2) Gráfico 2 (fila superior, columna derecha)
        crearGraficoPastel(drawing, hojaAux,
                1 + filas1 + 2, 1 + filas1 + 2 + filas2 - 1,
                "Mueble", "Cantidad Vendida", "Top 5 Muebles",
                baseColRight, baseRowTop,
                baseColRight + chartWidthCols, baseRowTop + chartHeightRows
        );

        // 3) Gráfico 3 (fila inferior, columna izquierda)
        crearGraficoBarras(drawing, hojaAux,
                1 + filas1 + filas2 + 4, 1 + filas1 + filas2 + 4 + filas3 - 1,
                "Mes", "Total Vendido", "Ventas Mensuales",
                baseColLeft, baseRowBottom,
                baseColLeft + chartWidthCols, baseRowBottom + chartHeightRows
        );

        // 4) Gráfico 4 (fila inferior, columna derecha)
        crearGraficoBarras(drawing, hojaAux,
                1 + filas1 + filas2 + filas3 + 6, 1 + filas1 + filas2 + filas3 + 6 + filas4 - 1,
                "Año", "Total Vendido", "Ventas Anuales",
                baseColRight, baseRowBottom,
                baseColRight + chartWidthCols, baseRowBottom + chartHeightRows
        );
    }
    private void crearGraficoBarras(XSSFDrawing drawing, XSSFSheet hojaDatos, int filaInicio, int filaFin,
                                    String tituloEjeX, String tituloEjeY, String tituloGrafico,
                                    int col1, int row1, int col2, int row2) {
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText(tituloGrafico);
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle(tituloEjeX);

        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(tituloEjeY);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> categorias = XDDFDataSourcesFactory.fromStringCellRange(
                hojaDatos, new CellRangeAddress(filaInicio, filaFin, 0, 0));
        XDDFNumericalDataSource<Double> valores = XDDFDataSourcesFactory.fromNumericCellRange(
                hojaDatos, new CellRangeAddress(filaInicio, filaFin, 1, 1));

        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFChartData.Series series = data.addSeries(categorias, valores);
        series.setTitle(tituloEjeY, null);

        chart.plot(data);

        ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
    }

    private void crearGraficoPastel(XSSFDrawing drawing, XSSFSheet hojaDatos, int filaInicio, int filaFin,
                                    String tituloEjeX, String tituloEjeY, String tituloGrafico,
                                    int col1, int row1, int col2, int row2) {
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.RIGHT);

        chart.setTitleText(tituloGrafico);
        chart.setTitleOverlay(false);

        XDDFDataSource<String> categorias = XDDFDataSourcesFactory.fromStringCellRange(
                hojaDatos, new CellRangeAddress(filaInicio, filaFin, 0, 0));
        XDDFNumericalDataSource<Double> valores = XDDFDataSourcesFactory.fromNumericCellRange(
                hojaDatos, new CellRangeAddress(filaInicio, filaFin, 1, 1));

        XDDFPieChartData data = (XDDFPieChartData) chart.createData(ChartTypes.PIE, null, null);
        XDDFPieChartData.Series series = (XDDFPieChartData.Series) data.addSeries(categorias, valores);
        series.setTitle(tituloGrafico, null);

        chart.plot(data);
    }
    private int contarFilasConDatos(XSSFSheet hoja, int filaInicio) {
        int fila = filaInicio;
        while (hoja.getRow(fila) != null &&
                hoja.getRow(fila).getCell(0) != null &&
                !hoja.getRow(fila).getCell(0).toString().isEmpty()) {
            fila++;
        }
        return fila - filaInicio;
    }
}
