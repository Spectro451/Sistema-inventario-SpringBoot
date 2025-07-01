package com.muebleria.inventario.service;


import com.muebleria.inventario.dto.ProveedorDTO;
import com.muebleria.inventario.dto.VentaDTO;
import com.muebleria.inventario.dto.VentaMuebleDTO;
import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.entidad.Venta;
import com.muebleria.inventario.entidad.VentaMueble;
import com.muebleria.inventario.repository.MuebleRepository;
import com.muebleria.inventario.repository.VentaMuebleRepository;
import com.muebleria.inventario.repository.VentaRepository;
import com.muebleria.inventario.util.ExcelStyleUtil;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VentaService {
    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private VentaMuebleService ventaMuebleService;

    @Autowired
    MuebleRepository muebleRepository;

    @Autowired
    VentaMuebleRepository ventaMuebleRepository;

    public List<Venta> findAll() {
        return ventaRepository.findAll();
    }

    public Optional<Venta> findById(Long id) {
        return ventaRepository.findById(id);
    }

    public void guardar(Venta venta) {
        ventaRepository.save(venta);
    }

    public void eliminar(Long id) {
        if (!ventaRepository.existsById(id)) {
            throw new RuntimeException("Mueble con id " + id + " no existe.");
        }
        ventaRepository.deleteById(id);
    }

    @Transactional
    public Venta guardarConDetalle(Venta venta) {

        Venta ventaSolo = new Venta();
        ventaSolo.setFecha(venta.getFecha() != null ? venta.getFecha() : LocalDate.now());
        ventaSolo.setTotal(0L);
        Venta ventaGuardada = ventaRepository.save(ventaSolo);

        Long total = 0L;
        List<VentaMueble> detallesGuardados = new ArrayList<>();

        for (VentaMueble vm : venta.getVentaMuebles()) {
            vm.setVenta(ventaGuardada);
            VentaMueble vmGuardado = ventaMuebleService.guardar(vm);
            detallesGuardados.add(vmGuardado);
            total += vmGuardado.getSubtotal();
        }

        ventaGuardada.setVentaMuebles(detallesGuardados);
        ventaGuardada.setTotal(total);

        return ventaRepository.save(ventaGuardada);
    }
    public List<VentaDTO> findAllDTO() {
        return ventaRepository.findAll().stream().map(venta -> {
            VentaDTO dto = new VentaDTO();
            dto.setId(venta.getId());
            dto.setFecha(venta.getFecha());
            dto.setTotal(venta.getTotal());

            List<VentaMuebleDTO> vmDTOs = venta.getVentaMuebles().stream().map(vm -> {
                VentaMuebleDTO vmDto = new VentaMuebleDTO();
                vmDto.setId(vm.getId());
                vmDto.setCantidad(vm.getCantidad());
                vmDto.setPrecioUnitario(vm.getPrecioUnitario());
                vmDto.setSubtotal(vm.getSubtotal());

                // Aquí traemos el nombre del mueble
                vmDto.setNombreMueble(vm.getMueble().getNombre());

                return vmDto;
            }).toList();

            dto.setVentaMuebles(vmDTOs);
            return dto;
        }).toList();
    }

    @Transactional
    public Venta update(Long id, Venta dto) {
        // 1) Cargo la venta existente y detalles actuales
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada id: " + id));

        venta.setFecha(dto.getFecha());

        // Mapa de detalles existentes (id -> VentaMueble)
        Map<Long, VentaMueble> existentes = venta.getVentaMuebles().stream()
                .collect(Collectors.toMap(VentaMueble::getId, Function.identity()));

        List<VentaMueble> procesados = new ArrayList<>();

        long totalNuevo = 0L;

        // 2) Recorro los detalles enviados para actualizar o crear
        for (VentaMueble vmDto : dto.getVentaMuebles()) {
            if (vmDto.getId() != null && existentes.containsKey(vmDto.getId())) {
                // 2a) Actualizar detalle existente y ajustar stock
                VentaMueble orig = existentes.remove(vmDto.getId());
                long cantidadVieja = orig.getCantidad();
                long cantidadNueva = vmDto.getCantidad();
                long diff = cantidadNueva - cantidadVieja;

                Mueble mueble = muebleRepository.findById(orig.getMueble().getId())
                        .orElseThrow(() -> new RuntimeException("Mueble no encontrado id: " + orig.getMueble().getId()));

                if (diff > 0 && mueble.getStock() < diff) {
                    throw new RuntimeException("Stock insuficiente para mueble: " + mueble.getNombre());
                }

                mueble.setStock(mueble.getStock() - (int) diff);
                muebleRepository.save(mueble);

                // Actualizar campos en el detalle
                orig.setCantidad(cantidadNueva);
                orig.setPrecioUnitario(mueble.getPrecioVenta());
                orig.setSubtotal(mueble.getPrecioVenta() * cantidadNueva);
                procesados.add(ventaMuebleRepository.save(orig));
                totalNuevo += orig.getSubtotal();

            } else if (vmDto.getCantidad() > 0) {
                // 2b) Crear nuevo detalle
                Mueble mueble = muebleRepository.findById(vmDto.getMueble().getId())
                        .orElseThrow(() -> new RuntimeException("Mueble no encontrado id: " + vmDto.getMueble().getId()));

                if (mueble.getStock() < vmDto.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente para mueble: " + mueble.getNombre());
                }

                mueble.setStock(mueble.getStock() - vmDto.getCantidad());
                muebleRepository.save(mueble);

                vmDto.setVenta(venta);
                vmDto.setPrecioUnitario(mueble.getPrecioVenta());
                vmDto.setSubtotal(mueble.getPrecioVenta() * vmDto.getCantidad());

                VentaMueble nuevo = ventaMuebleRepository.save(vmDto);
                procesados.add(nuevo);
                totalNuevo += nuevo.getSubtotal();
            }
        }

        existentes.values().forEach(detalle -> {
            // Antes de eliminar, devolver stock del mueble
            Mueble mueble = muebleRepository.findById(detalle.getMueble().getId())
                    .orElseThrow(() -> new RuntimeException("Mueble no encontrado id: " + detalle.getMueble().getId()));

            mueble.setStock(mueble.getStock() + detalle.getCantidad());
            muebleRepository.save(mueble);

            ventaMuebleRepository.delete(detalle);
        });

        // 4) Actualizar la lista y total en la venta
        venta.getVentaMuebles().clear();
        venta.getVentaMuebles().addAll(procesados);
        venta.setTotal(totalNuevo);

        // 5) Guardar y devolver
        return ventaRepository.save(venta);
    }

    public byte[] generarReporteVenta() throws IOException {
        List<VentaDTO> ventas = findAllDTO();
        Map<String, Integer> conteoMuebles = new LinkedHashMap<>();

        for (VentaDTO venta : ventas) {
            if (venta.getVentaMuebles() != null) {
                for (VentaMuebleDTO vm : venta.getVentaMuebles()) {
                    conteoMuebles.merge(vm.getNombreMueble(), vm.getCantidad().intValue(), Integer::sum);
                }
            }
        }

        // Para ventas por mes y año
        Map<YearMonth, Integer> ventasPorMes = new TreeMap<>();
        Map<Integer, Integer> ventasPorAño = new TreeMap<>();

        for (VentaDTO venta : ventas) {
            LocalDate fecha = LocalDate.parse(venta.getFecha().toString());
            YearMonth ym = YearMonth.from(fecha);
            int año = fecha.getYear();

            ventasPorMes.merge(ym, venta.getTotal().intValue(), Integer::sum);
            ventasPorAño.merge(año, venta.getTotal().intValue(), Integer::sum);
        }

        // Sacar top 5 muebles
        List<Map.Entry<String, Integer>> top5Muebles = conteoMuebles.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = ExcelStyleUtil.crearEstiloTitulo(workbook);
            CellStyle estiloDatos = ExcelStyleUtil.crearEstiloDatos(workbook);
            CellStyle estiloFecha = ExcelStyleUtil.crearEstiloFecha(workbook);
            CellStyle estiloMoneda = ExcelStyleUtil.crearEstiloMoneda(workbook);


            // Crear estilo con wrapText para materiales con saltos de línea
            CellStyle estiloWrap = workbook.createCellStyle();
            estiloWrap.cloneStyleFrom(estiloDatos);
            estiloWrap.setWrapText(true);

            Sheet hojaDatos = workbook.createSheet("Reportes Venta");

            // Encabezados
            Row header = hojaDatos.createRow(0);
            String[] columnas = {"ID", "Fecha", "Mueble", "Total"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue("   " + columnas[i] + "   ");
                cell.setCellStyle(estiloTitulo);
            }

            int rowNum = 1;
            for (VentaDTO venta : ventas) {
                Row row = hojaDatos.createRow(rowNum++);
                row.createCell(0).setCellValue(venta.getId());
                row.createCell(1).setCellValue(venta.getFecha());
                row.createCell(3).setCellValue(venta.getTotal());

                String nombresMaterial = "";
                if (venta.getVentaMuebles() != null) {
                    nombresMaterial = venta.getVentaMuebles().stream()
                            .map(mm -> String.format("%s: %d unidades ($%d c/u)",
                                    mm.getNombreMueble(),
                                    mm.getCantidad(),
                                    mm.getPrecioUnitario()
                            ))
                            .distinct()
                            .collect(Collectors.joining("\n"));
                }

                Cell materialesCell = row.createCell(2);
                materialesCell.setCellValue(nombresMaterial);
                materialesCell.setCellStyle(estiloWrap);

                for (int i = 0; i <= 3; i++) {
                    Cell c = row.getCell(i);
                    if (i == 2) {
                        c.setCellStyle(estiloWrap);
                    } else if (i == 1) {
                        c.setCellStyle(estiloFecha);
                    } else if (i==3) {
                        c.setCellStyle(estiloMoneda);
                    } else {
                        c.setCellStyle(estiloDatos);
                    }
                }
            }
            int ultimaColumna = columnas.length - 1;
            hojaDatos.setAutoFilter(new CellRangeAddress(
                    0,
                    ventas.size(),
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

            XSSFSheet hojaAux = (XSSFSheet) workbook.createSheet("aux_graf");
            int fila = 0;

            //Ventas por mueble
            Row encabezadoMuebles = hojaAux.createRow(fila++);
            encabezadoMuebles.createCell(0).setCellValue("Mueble");
            encabezadoMuebles.createCell(1).setCellValue("Cantidad Vendida");

            for (Map.Entry<String, Integer> entry : conteoMuebles.entrySet()) {
                Row filaDatos = hojaAux.createRow(fila++);
                filaDatos.createCell(0).setCellValue(entry.getKey());
                filaDatos.createCell(1).setCellValue(entry.getValue());
            }

            fila++; // fila vacía

            // Top Venta
            Row encabezadoTop5 = hojaAux.createRow(fila++);
            encabezadoTop5.createCell(0).setCellValue("Top 5 Muebles");
            encabezadoTop5.createCell(1).setCellValue("Cantidad Vendida");

            for (Map.Entry<String, Integer> entry : top5Muebles) {
                Row filaTop5 = hojaAux.createRow(fila++);
                filaTop5.createCell(0).setCellValue(entry.getKey());
                filaTop5.createCell(1).setCellValue(entry.getValue());
            }

            fila++; // fila vacía

            // Mensual
            Row encabezadoMes = hojaAux.createRow(fila++);
            encabezadoMes.createCell(0).setCellValue("Mes");
            encabezadoMes.createCell(1).setCellValue("Total Vendido");

            YearMonth primerMes = ventasPorMes.keySet().stream().min(Comparator.naturalOrder()).orElse(YearMonth.now());
            YearMonth ultimoMes = ventasPorMes.keySet().stream().max(Comparator.naturalOrder()).orElse(YearMonth.now());

            DateTimeFormatter formato = DateTimeFormatter.ofPattern("MMM yyyy", new Locale("es", "ES"));
            YearMonth mesIter = primerMes;
            while (!mesIter.isAfter(ultimoMes)) {
                Row filaMes = hojaAux.createRow(fila++);
                filaMes.createCell(0).setCellValue(mesIter.format(formato));
                filaMes.createCell(1).setCellValue(ventasPorMes.getOrDefault(mesIter, 0));
                mesIter = mesIter.plusMonths(1);
            }

            fila++; // fila vacía

            // Anual
            Row encabezadoAnio = hojaAux.createRow(fila++);
            encabezadoAnio.createCell(0).setCellValue("Año");
            encabezadoAnio.createCell(1).setCellValue("Total Vendido");

            int primerAnio = ventasPorAño.keySet().stream().min(Comparator.naturalOrder()).orElse(LocalDate.now().getYear());
            int ultimoAnio = ventasPorAño.keySet().stream().max(Comparator.naturalOrder()).orElse(LocalDate.now().getYear());

            for (int anioIter = primerAnio; anioIter <= ultimoAnio; anioIter++) {
                Row filaAnio = hojaAux.createRow(fila++);
                filaAnio.createCell(0).setCellValue(anioIter);
                filaAnio.createCell(1).setCellValue(ventasPorAño.getOrDefault(anioIter, 0));
            }

            // 🎯 Ocultar hoja auxiliar para no molestar en el Excel final
            int idx = workbook.getSheetIndex(hojaAux);
            workbook.setSheetVisibility(idx, SheetVisibility.VERY_HIDDEN);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
