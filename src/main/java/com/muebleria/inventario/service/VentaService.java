package com.muebleria.inventario.service;


import com.muebleria.inventario.dto.VentaDTO;
import com.muebleria.inventario.dto.VentaMuebleDTO;
import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.entidad.Venta;
import com.muebleria.inventario.entidad.VentaMueble;
import com.muebleria.inventario.repository.MuebleRepository;
import com.muebleria.inventario.repository.VentaMuebleRepository;
import com.muebleria.inventario.repository.VentaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        ventaSolo.setFecha(LocalDate.now());
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
            // si vmDto tiene id y cantidad=0, lo eliminaremos en siguiente paso
        }

        // 3) Eliminar detalles que no vienen en dto (ya no existen)
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
}
