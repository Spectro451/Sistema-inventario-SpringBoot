package com.muebleria.inventario.service;


import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.entidad.Venta;
import com.muebleria.inventario.entidad.VentaMueble;
import com.muebleria.inventario.repository.MuebleRepository;
import com.muebleria.inventario.repository.VentaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VentaService {
    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private VentaMuebleService ventaMuebleService;

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
}
