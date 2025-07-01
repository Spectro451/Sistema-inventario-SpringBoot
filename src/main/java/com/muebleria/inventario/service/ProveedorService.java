package com.muebleria.inventario.service;

import com.muebleria.inventario.dto.*;
import com.muebleria.inventario.entidad.Material;
import com.muebleria.inventario.entidad.Proveedor;
import com.muebleria.inventario.entidad.ProveedorMateriales;
import com.muebleria.inventario.entidad.TipoMaterial;
import com.muebleria.inventario.repository.MaterialRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProveedorService {
    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProveedorMaterialesRepository proveedorMaterialesRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ProveedorMaterialesService proveedorMaterialesService;

    

    public Optional<Proveedor> getProveedoresId(Long id) {
        return proveedorRepository.findById(id);
    }

    @Transactional
    public ProveedorDTO guardarProveedor(ProveedorDTO proveedorDTO) {
        // 1) Guardar entidad Proveedor base
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(proveedorDTO.getNombre());
        proveedor.setTelefono(proveedorDTO.getTelefono());
        proveedor.setCorreo(proveedorDTO.getCorreo());
        proveedor.setDireccion(proveedorDTO.getDireccion());
        proveedor.setProveedorMateriales(new ArrayList<>());
        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);

        // 2) Actualizar el DTO con el ID generado
        proveedorDTO.setId(proveedorGuardado.getId());

        // 3) Procesar cada ProveedorMaterialDTO y rellenar su ID
        if (proveedorDTO.getProveedorMateriales() != null) {
            for (ProveedorMaterialDTO pmDTO : proveedorDTO.getProveedorMateriales()) {
                // 3.1 Obtener o crear Material
                MaterialSimpleDTO matDTO = pmDTO.getMaterial();
                Material material;
                if (matDTO.getId() != null) {
                    material = materialRepository.findById(matDTO.getId())
                            .orElseThrow(() -> new RuntimeException("Material no encontrado con id: " + matDTO.getId()));
                } else {
                    // busca por nombre/tipo o crea uno nuevo
                    material = materialRepository
                            .findByNombreAndTipo(matDTO.getNombre(),
                                    TipoMaterial.valueOf(matDTO.getTipo().toUpperCase()))
                            .orElseGet(() -> {
                                Material nuevo = new Material();
                                nuevo.setNombre(matDTO.getNombre());
                                nuevo.setTipo(TipoMaterial.valueOf(matDTO.getTipo().toUpperCase()));
                                nuevo.setDescripcion(matDTO.getDescripcion());
                                nuevo.setUnidadDeMedida(matDTO.getUnidadDeMedida());
                                nuevo.setStockActual(0L);
                                return materialRepository.save(nuevo);
                            });
                }
                // actualizar DTO de material con su ID (nuevo o existente)
                matDTO.setId(material.getId());

                // 3.2 Verificar si ya existía la relación
                ProveedorMateriales existente = proveedorMaterialesRepository
                        .findByProveedor_IdAndMaterial_Id(proveedorGuardado.getId(), material.getId());

                if (existente == null) {
                    // crear nueva relación
                    ProveedorMateriales nuevaRel = new ProveedorMateriales();
                    nuevaRel.setProveedor(proveedorGuardado);
                    nuevaRel.setMaterial(material);
                    nuevaRel.setCostoUnitario(pmDTO.getCostoUnitario());
                    nuevaRel.setCantidadSuministrada(0L);
                    ProveedorMateriales guardada = proveedorMaterialesRepository.save(nuevaRel);
                    // 3.3 Rellenar el ID de la relación en el DTO
                    pmDTO.setId(guardada.getId());
                } else {
                    // ya existía: rellenamos su ID en el DTO
                    pmDTO.setId(existente.getId());
                }
            }
        }

        // 4) Al final devolvemos el mismo proveedorDTO, ahora con todos los IDs y datos listos
        return proveedorDTO;
    }


    @Transactional
    public void eliminarProveedor(Long proveedorId) {
        // Verificar existencia
        if (!proveedorRepository.existsById(proveedorId)) {
            throw new RuntimeException("Proveedor con id " + proveedorId + " no existe.");
        }

        // Obtener todas las relaciones ProveedorMateriales para ese proveedor
        List<ProveedorMateriales> relaciones = proveedorMaterialesRepository.findByProveedorId(proveedorId);

        // Eliminar relaciones proveedor-material
        for (ProveedorMateriales pm : relaciones) {
            proveedorMaterialesRepository.delete(pm);
        }

        // Finalmente eliminar proveedor
        proveedorRepository.deleteById(proveedorId);
    }

    public List<ProveedorDTO> findAllDTO() {
        List<Proveedor> proveedores = proveedorRepository.findAll();

        return proveedores.stream().map(proveedor -> {
            ProveedorDTO proveedorDTO = new ProveedorDTO();
            proveedorDTO.setId(proveedor.getId());
            proveedorDTO.setNombre(proveedor.getNombre());
            proveedorDTO.setTelefono(proveedor.getTelefono());
            proveedorDTO.setCorreo(proveedor.getCorreo());
            proveedorDTO.setDireccion(proveedor.getDireccion());

            if (proveedor.getProveedorMateriales() != null) {
                List<ProveedorMaterialDTO> pmDTOs = proveedor.getProveedorMateriales().stream().map(pm -> {
                    ProveedorMaterialDTO pmDTO = new ProveedorMaterialDTO();
                    pmDTO.setId(pm.getId());
                    pmDTO.setCostoUnitario(pm.getCostoUnitario());
                    pmDTO.setCantidadSuministrada(pm.getCantidadSuministrada());

                    Material material = pm.getMaterial();
                    MaterialSimpleDTO materialSimpleDTO = new MaterialSimpleDTO();
                    materialSimpleDTO.setId(material.getId());
                    materialSimpleDTO.setNombre(material.getNombre());
                    materialSimpleDTO.setTipo(material.getTipo().toString()); // Si usas enum
                    materialSimpleDTO.setDescripcion(material.getDescripcion());
                    materialSimpleDTO.setUnidadDeMedida(material.getUnidadDeMedida());
                    materialSimpleDTO.setStockActual(material.getStockActual());

                    pmDTO.setMaterial(materialSimpleDTO);

                    return pmDTO;
                }).collect(Collectors.toList());

                proveedorDTO.setProveedorMateriales(pmDTOs);
            } else {
                proveedorDTO.setProveedorMateriales(new ArrayList<>());
            }

            return proveedorDTO;
        }).collect(Collectors.toList());
    }

    @Transactional
    public ProveedorDTO actualizarProveedor(Long id, ProveedorDTO proveedorDTO) {
        // 1) Cargar proveedor existente
        Proveedor existente = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id: " + id));

        // 2) Actualizar campos básicos
        existente.setNombre(proveedorDTO.getNombre());
        existente.setTelefono(proveedorDTO.getTelefono());
        existente.setCorreo(proveedorDTO.getCorreo());
        existente.setDireccion(proveedorDTO.getDireccion());

        // 3) Map de relaciones actuales (pendientes de eliminar)
        Map<Long, ProveedorMateriales> pendientes = existente.getProveedorMateriales()
                .stream()
                .collect(Collectors.toMap(ProveedorMateriales::getId, Function.identity()));

        List<ProveedorMateriales> resultado = new ArrayList<>();

        // 4) Procesar cada ProveedorMaterialDTO entrante
        if (proveedorDTO.getProveedorMateriales() != null) {
            for (ProveedorMaterialDTO pmDTO : proveedorDTO.getProveedorMateriales()) {
                ProveedorMateriales rel;

                if (pmDTO.getId() != null && pendientes.containsKey(pmDTO.getId())) {
                    // 4a) RELACIÓN EXISTENTE → actualizar costo y cantidad
                    rel = pendientes.remove(pmDTO.getId());
                    rel.setCostoUnitario(pmDTO.getCostoUnitario());
                    rel.setCantidadSuministrada(pmDTO.getCantidadSuministrada());

                    // El material no debería cambiar, pero si quieres manejarlo, puedes agregar lógica aquí

                } else {
                    // 4b) NUEVA RELACIÓN → crear relación nueva
                    rel = new ProveedorMateriales();
                    rel.setProveedor(existente);

                    // 4b.1) Obtener o crear Material como en guardarProveedor
                    MaterialSimpleDTO matDTO = pmDTO.getMaterial();
                    Material material;

                    if (matDTO.getId() != null) {
                        material = materialRepository.findById(matDTO.getId())
                                .orElseThrow(() -> new RuntimeException("Material no encontrado con id: " + matDTO.getId()));
                    } else {
                        material = materialRepository
                                .findByNombreAndTipo(matDTO.getNombre(),
                                        TipoMaterial.valueOf(matDTO.getTipo().toUpperCase()))
                                .orElseGet(() -> {
                                    Material nuevo = new Material();
                                    nuevo.setNombre(matDTO.getNombre());
                                    nuevo.setTipo(TipoMaterial.valueOf(matDTO.getTipo().toUpperCase()));
                                    nuevo.setDescripcion(matDTO.getDescripcion());
                                    nuevo.setUnidadDeMedida(matDTO.getUnidadDeMedida());
                                    nuevo.setStockActual(0L);
                                    return materialRepository.save(nuevo);
                                });

                        // Actualizar DTO con nuevo id generado
                        matDTO.setId(material.getId());
                    }

                    rel.setMaterial(material);
                    rel.setCostoUnitario(pmDTO.getCostoUnitario());
                    rel.setCantidadSuministrada(pmDTO.getCantidadSuministrada());

                    ProveedorMateriales creado = proveedorMaterialesService.guardarOActualizar(rel);
                    resultado.add(creado);
                    continue; // saltar el add al final del ciclo para evitar duplicados
                }

                resultado.add(proveedorMaterialesService.guardarOActualizar(rel));
            }
        }

        // 5) ELIMINAR relaciones que quedaron en 'pendientes'
        for (ProveedorMateriales aBorrar : pendientes.values()) {
            proveedorMaterialesRepository.delete(aBorrar);
        }

        // 6) Sincronizar la colección y guardar el proveedor actualizado
        existente.getProveedorMateriales().clear();
        existente.getProveedorMateriales().addAll(resultado);

        Proveedor proveedorGuardado = proveedorRepository.save(existente);

        // 7) Mapear ENTIDAD a DTO para devolver resultado actualizado
        ProveedorDTO salida = new ProveedorDTO();
        salida.setId(proveedorGuardado.getId());
        salida.setNombre(proveedorGuardado.getNombre());
        salida.setTelefono(proveedorGuardado.getTelefono());
        salida.setCorreo(proveedorGuardado.getCorreo());
        salida.setDireccion(proveedorGuardado.getDireccion());

        salida.setProveedorMateriales(
                proveedorGuardado.getProveedorMateriales().stream().map(pm -> {
                    ProveedorMaterialDTO dto = new ProveedorMaterialDTO();
                    dto.setId(pm.getId());
                    dto.setCostoUnitario(pm.getCostoUnitario());
                    dto.setCantidadSuministrada(pm.getCantidadSuministrada());

                    MaterialSimpleDTO ms = new MaterialSimpleDTO();
                    ms.setId(pm.getMaterial().getId());
                    ms.setNombre(pm.getMaterial().getNombre());
                    ms.setTipo(pm.getMaterial().getTipo().name());
                    ms.setDescripcion(pm.getMaterial().getDescripcion());
                    ms.setUnidadDeMedida(pm.getMaterial().getUnidadDeMedida());
                    ms.setStockActual(pm.getMaterial().getStockActual());

                    dto.setMaterial(ms);
                    return dto;
                }).collect(Collectors.toList())
        );

        return salida;
    }

    public byte[] generarReporteProveedor() throws IOException {
        List<ProveedorDTO> proveedores = findAllDTO();

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloTitulo = ExcelStyleUtil.crearEstiloTitulo(workbook);
            CellStyle estiloDatos = ExcelStyleUtil.crearEstiloDatos(workbook);
            CellStyle estiloFecha = ExcelStyleUtil.crearEstiloFecha(workbook);
            CellStyle estiloMoneda = ExcelStyleUtil.crearEstiloMoneda(workbook);

            // Crear estilo con wrapText para materiales con saltos de línea
            CellStyle estiloWrap = workbook.createCellStyle();
            estiloWrap.cloneStyleFrom(estiloDatos);
            estiloWrap.setWrapText(true);

            Sheet hojaDatos = workbook.createSheet("Reporte Proveedor");

            // Encabezados
            Row header = hojaDatos.createRow(0);
            String[] columnas = {"ID", "Nombre", "Telefono", "Correo", "Direccion", "Materiales"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue("   " + columnas[i] + "   ");
                cell.setCellStyle(estiloTitulo);
            }

            int rowNum = 1;
            for (ProveedorDTO proveedor : proveedores) {
                Row row = hojaDatos.createRow(rowNum++);
                row.createCell(0).setCellValue(proveedor.getId());
                row.createCell(1).setCellValue(proveedor.getNombre());
                row.createCell(2).setCellValue(proveedor.getTelefono());
                row.createCell(3).setCellValue(proveedor.getCorreo());
                row.createCell(4).setCellValue(proveedor.getDireccion());

                String nombresMaterial = "";
                if (proveedor.getProveedorMateriales() != null) {
                    nombresMaterial = proveedor.getProveedorMateriales().stream()
                            .map(mm -> String.format("%s: %d unidades ($%d c/u)",
                                    mm.getMaterial().getNombre(),
                                    mm.getCantidadSuministrada(),
                                    mm.getCostoUnitario()
                            ))
                            .distinct()
                            .collect(Collectors.joining("\n"));
                }

                Cell materialesCell = row.createCell(5);
                materialesCell.setCellValue(nombresMaterial);
                materialesCell.setCellStyle(estiloWrap);

                for (int i = 0; i <= 5; i++) {
                    Cell c = row.getCell(i);
                    if (i == 5) {
                        c.setCellStyle(estiloWrap); // texto con wrap
                    } else {
                        c.setCellStyle(estiloDatos); // resto celdas
                    }
                }
            }
            int ultimaColumna = columnas.length - 1;
            hojaDatos.setAutoFilter(new CellRangeAddress(
                    0,
                    proveedores.size(),
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
