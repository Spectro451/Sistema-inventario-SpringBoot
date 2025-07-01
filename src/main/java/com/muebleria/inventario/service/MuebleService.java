package com.muebleria.inventario.service;


import com.muebleria.inventario.dto.MaterialDTO;
import com.muebleria.inventario.dto.MaterialMuebleDTO;
import com.muebleria.inventario.dto.MaterialSimpleDTO;
import com.muebleria.inventario.dto.MuebleDTO;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.MaterialMueble;
import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.repository.MaterialMuebleRepository;
import com.muebleria.inventario.repository.MaterialRepository;
import com.muebleria.inventario.repository.MuebleRepository;
import com.muebleria.inventario.util.ExcelStyleUtil;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MuebleService {

    @Autowired
    MuebleRepository muebleRepository;

    @Autowired
    MaterialMuebleService materialMuebleService;

    @Autowired
    MaterialMuebleRepository materialMuebleRepository;

    @Autowired
    MaterialRepository materialRepository;

    public List<Mueble> findAll() {

        return muebleRepository.findAll();
    }

    public MuebleDTO findById(Long id) {
        Mueble mueble = muebleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + id));

        MuebleDTO muebleDTO = new MuebleDTO();
        muebleDTO.setId(mueble.getId());
        muebleDTO.setNombre(mueble.getNombre());
        muebleDTO.setDescripcion(mueble.getDescripcion());
        muebleDTO.setPrecioVenta(mueble.getPrecioVenta());
        muebleDTO.setStock(mueble.getStock());

        if (mueble.getMaterialMuebles() != null) {
            List<MaterialMuebleDTO> mmDTOs = mueble.getMaterialMuebles().stream().map(mm -> {
                MaterialMuebleDTO mmDTO = new MaterialMuebleDTO();
                mmDTO.setId(mm.getId());
                mmDTO.setCantidadUtilizada(mm.getCantidadUtilizada());

                Material material = mm.getMaterial();
                MaterialSimpleDTO materialDTO = new MaterialSimpleDTO();
                materialDTO.setId(material.getId());
                materialDTO.setNombre(material.getNombre());
                materialDTO.setTipo(material.getTipo().toString());
                materialDTO.setDescripcion(material.getDescripcion());
                materialDTO.setUnidadDeMedida(material.getUnidadDeMedida());
                materialDTO.setStockActual(material.getStockActual());

                mmDTO.setMaterial(materialDTO);

                return mmDTO;
            }).collect(Collectors.toList());

            muebleDTO.setMaterialMuebles(mmDTOs);
        } else {
            muebleDTO.setMaterialMuebles(new ArrayList<>());
        }

        return muebleDTO;
    }

    @Transactional
    public void delete(Long id) {
        Mueble mueble = muebleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mueble no encontrado con id: " + id));
        if (!mueble.getVentaMuebles().isEmpty()) {
            throw new RuntimeException("No se puede borrar el mueble porque tiene ventas asociadas.");
        }
        muebleRepository.delete(mueble);
    }

    @Transactional
    public Mueble update(Long id, Mueble dto) {
        // 1) Cargo entidad y guardo stock viejo
        Mueble m = muebleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mueble no encontrado id: " + id));
        Long stockViejo = m.getStock();
        Long stockNuevo = dto.getStock();

        // 2) Actualizo campos básicos
        m.setNombre(dto.getNombre());
        m.setDescripcion(dto.getDescripcion());
        m.setPrecioVenta(dto.getPrecioVenta());
        m.setStock(stockNuevo);

        // 3) Preparo mapas y listas para relaciones
        Map<Long, MaterialMueble> existentes = m.getMaterialMuebles().stream()
                .filter(mm -> mm.getId() != null)
                .collect(Collectors.toMap(MaterialMueble::getId, Function.identity()));
        List<MaterialMueble> procesados = new ArrayList<>();

        // 4) Actualizo o creo cada MaterialMueble del DTO
        for (MaterialMueble mmDto : dto.getMaterialMuebles()) {
            if (mmDto.getCantidadUtilizada() == 0) {
                // Si la cantidad es 0 y la relación existe, devolver stock y eliminarla
                if (mmDto.getId() != null && existentes.containsKey(mmDto.getId())) {
                    MaterialMueble toDelete = existentes.remove(mmDto.getId());
                    Material mat = toDelete.getMaterial();

                    // Devolver stock según cantidad usada * stock del mueble
                    long devolver = toDelete.getCantidadUtilizada() * stockNuevo;
                    mat.setStockActual(mat.getStockActual() + devolver);
                    materialRepository.save(mat);

                    // Eliminar relación
                    materialMuebleRepository.delete(toDelete);
                }
                // No agregar a procesados ni hacer nada más
                continue;
            }

            if (mmDto.getId() != null && existentes.containsKey(mmDto.getId())) {
                // 4a) actualizar existente y ajustar stock si cambió la cantidad usada
                MaterialMueble orig = existentes.remove(mmDto.getId());
                long vieja = orig.getCantidadUtilizada();
                long nueva = mmDto.getCantidadUtilizada();
                long diff = nueva - vieja;
                Material mat = orig.getMaterial();

                if (diff != 0 && stockNuevo > 0) {
                    long ajuste = diff * stockNuevo;
                    if (ajuste > 0 && mat.getStockActual() < ajuste) {
                        throw new RuntimeException("Stock insuficiente de " + mat.getNombre() + " para el ajuste");
                    }
                    mat.setStockActual(mat.getStockActual() - ajuste);
                    materialRepository.save(mat);
                }

                orig.setCantidadUtilizada(nueva);
                procesados.add(materialMuebleService.update(orig));

            } else if (mmDto.getId() != null && !existentes.containsKey(mmDto.getId())) {

                MaterialMueble posibleHuérfano = materialMuebleRepository.findById(mmDto.getId()).orElse(null);
                if (posibleHuérfano != null) {
                    long vieja = posibleHuérfano.getCantidadUtilizada();
                    long nueva = mmDto.getCantidadUtilizada();
                    long diff = nueva - vieja;
                    Material mat = posibleHuérfano.getMaterial();

                    if (diff != 0 && stockNuevo > 0) {
                        long ajuste = diff * stockNuevo;
                        if (ajuste > 0 && mat.getStockActual() < ajuste) {
                            throw new RuntimeException("Stock insuficiente de " + mat.getNombre() + " para el ajuste");
                        }
                        mat.setStockActual(mat.getStockActual() - ajuste);
                        materialRepository.save(mat);
                    }

                    posibleHuérfano.setCantidadUtilizada(nueva);
                    posibleHuérfano.setMueble(m); // re-asociar mueble por si falta
                    procesados.add(materialMuebleService.update(posibleHuérfano));
                } else {
                    throw new RuntimeException("MaterialMueble con id=" + mmDto.getId() + " no existe");
                }

            } else if (mmDto.getCantidadUtilizada() > 0) {
                // 4b) crear nuevo
                mmDto.setMueble(m);
                procesados.add(materialMuebleService.update(mmDto));
            }
        }

        // 5) Elimino las relaciones que quedaron fuera
        existentes.values().forEach(materialMuebleRepository::delete);

        // 6) Reemplazo la colección en el mueble
        m.getMaterialMuebles().clear();
        m.getMaterialMuebles().addAll(procesados);

        // 7) Ajusto stock de materiales por cambio de unidades del mueble
        long unidadesDiff = stockNuevo - stockViejo;
        if (unidadesDiff != 0) {
            for (MaterialMueble mm : procesados) {
                Material mat = mm.getMaterial();
                long consumo = mm.getCantidadUtilizada() * unidadesDiff;
                if (consumo > 0 && mat.getStockActual() < consumo) {
                    throw new RuntimeException(
                            "Stock insuficiente de " + mat.getNombre() +
                                    " para producir " + unidadesDiff + " unidades");
                }
                mat.setStockActual(mat.getStockActual() - consumo);
                materialRepository.save(mat);
            }
        }

        // 8) Guardo y retorno
        return muebleRepository.save(m);
    }

    @Transactional
    public Mueble guardarConDetalle(Mueble mueble) {
        // Ignoramos cualquier id que venga del cliente
        Mueble muebleSolo = new Mueble();
        muebleSolo.setNombre(mueble.getNombre());
        muebleSolo.setDescripcion(mueble.getDescripcion());
        muebleSolo.setPrecioVenta(mueble.getPrecioVenta());
        muebleSolo.setStock(mueble.getStock());

        // Guardamos mueble nuevo (se crea con id nuevo)
        Mueble muebleGuardado = muebleRepository.save(muebleSolo);

        List<MaterialMueble> relacionesGuardadas = new ArrayList<>();

        if (mueble.getMaterialMuebles() != null) {
            for (MaterialMueble mm : mueble.getMaterialMuebles()) {
                mm.setId(null); // FORZAR creación NUEVA relación, ignorar cualquier id previo
                mm.setMueble(muebleGuardado);
                MaterialMueble guardado = materialMuebleService.guardar(mm, mueble.getStock()); // Aquí se hace la validación y descuento stock
                relacionesGuardadas.add(guardado);
            }
        }

        // Actualizar la lista de relaciones en el mueble guardado (no es obligatorio si solo se usa el id)
        muebleGuardado.setMaterialMuebles(relacionesGuardadas);

        return muebleGuardado;
    }

    public List<MuebleDTO> findAllDTO() {
        List<Mueble> muebles = muebleRepository.findAll();

        return muebles.stream().map(mueble -> {
            MuebleDTO muebleDTO = new MuebleDTO();
            muebleDTO.setId(mueble.getId());
            muebleDTO.setNombre(mueble.getNombre());
            muebleDTO.setDescripcion(mueble.getDescripcion());
            muebleDTO.setPrecioVenta(mueble.getPrecioVenta());
            muebleDTO.setStock(mueble.getStock());

            if (mueble.getMaterialMuebles() != null) {
                List<MaterialMuebleDTO> mmDTOs = mueble.getMaterialMuebles().stream()
                        .filter(mm -> mm.getCantidadUtilizada() > 0) // <--- solo los que tienen cantidad > 0
                        .map(mm -> {
                    MaterialMuebleDTO mmDTO = new MaterialMuebleDTO();
                    mmDTO.setId(mm.getId());
                    mmDTO.setCantidadUtilizada(mm.getCantidadUtilizada());

                    Material material = mm.getMaterial();
                    MaterialSimpleDTO materialDTO = new MaterialSimpleDTO();
                    materialDTO.setId(material.getId());
                    materialDTO.setNombre(material.getNombre());
                    materialDTO.setTipo(material.getTipo().toString());
                    materialDTO.setDescripcion(material.getDescripcion());
                    materialDTO.setUnidadDeMedida(material.getUnidadDeMedida());
                    materialDTO.setStockActual(material.getStockActual());

                    mmDTO.setMaterial(materialDTO);

                    return mmDTO;
                }).collect(Collectors.toList());

                muebleDTO.setMaterialMuebles(mmDTOs);
            } else {
                muebleDTO.setMaterialMuebles(new ArrayList<>());
            }

            return muebleDTO;
        }).collect(Collectors.toList());
    }

    public byte[] generarReporteMueble() throws IOException {
        List<MuebleDTO> muebles = findAllDTO();

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = ExcelStyleUtil.crearEstiloTitulo(workbook);
            CellStyle estiloDatos = ExcelStyleUtil.crearEstiloDatos(workbook);
            CellStyle estiloStockBajo = ExcelStyleUtil.crearEstiloStockBajo(workbook);
            CellStyle estiloWrap = ExcelStyleUtil.crearEstiloWrap(workbook);
            CellStyle estiloMoneda = ExcelStyleUtil.crearEstiloMoneda(workbook);

            Sheet hojaDatos = workbook.createSheet("Reporte Mueble");

            // Encabezados
            Row header = hojaDatos.createRow(0);
            String[] columnas = {"ID", "Nombre", "Descripcion", "Precio Venta", "Stock", "Materiales Usados"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue("   " + columnas[i] + "   ");
                cell.setCellStyle(estiloTitulo); // Aplico estilo al encabezado
            }

            int rowNum = 1;
            for (MuebleDTO mueble : muebles) {
                Row row = hojaDatos.createRow(rowNum++);
                row.createCell(0).setCellValue(mueble.getId());
                row.createCell(1).setCellValue(mueble.getNombre());
                row.createCell(2).setCellValue(mueble.getDescripcion());
                row.createCell(3).setCellValue(mueble.getPrecioVenta());
                row.createCell(4).setCellValue(mueble.getStock());

                String nombresMaterial = "";
                if (mueble.getMaterialMuebles() != null) {
                    nombresMaterial = mueble.getMaterialMuebles().stream()
                            .map(mm -> String.format("%s: %d unidades",
                                    mm.getMaterial().getNombre(),
                                    mm.getCantidadUtilizada()
                            ))
                            .distinct()
                            .collect(Collectors.joining("\n"));
                }

                row.createCell(5).setCellValue(nombresMaterial);

                // Aplico el estilo a todas las celdas
                for (int i = 0; i <= 5; i++) {
                    Cell c = row.getCell(i);
                    if (i == 3) {
                        c.setCellStyle(estiloMoneda); // fecha con formato y estilo general
                    } else if (i == 4) {
                        if (mueble.getStock() <= 10){
                            c.setCellStyle(estiloStockBajo);
                        } else {
                            c.setCellStyle(estiloDatos);
                        }

                    } else if (i == 5) {
                        c.setCellStyle(estiloWrap);
                    } else {
                        c.setCellStyle(estiloDatos); // resto celdas
                    }
                }
            }
            int ultimaColumna = columnas.length - 1;
            hojaDatos.setAutoFilter(new CellRangeAddress(
                    0,
                    muebles.size(),
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
}
