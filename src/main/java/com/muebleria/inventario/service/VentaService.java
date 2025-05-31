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

    @Autowired
    private MuebleRepository muebleRepository;

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
        ventaRepository.deleteById(id);
    }

    @Transactional
    public Venta guardarConDetalle(Venta venta) {
        // 1) Guarda la Venta sin pasarle la lista de ventaMuebles:
        //    Para ello, “clonamos” la venta pero con lista vacía:
        Venta ventaSolo = new Venta();
        ventaSolo.setFecha(LocalDate.now());
        ventaSolo.setTotal(0L);
        Venta ventaGuardada = ventaRepository.save(ventaSolo);

        // 2) Ahora recorremos cada VentaMueble que vino en el JSON
        //    (estos objetos todavía no se guardaron en BD).
        Long total = 0L;
        List<VentaMueble> detallesGuardados = new ArrayList<>();

        for (VentaMueble vm : venta.getVentaMuebles()) {
            vm.setVenta(ventaGuardada); // asignamos la venta que ya existe en BD
            VentaMueble vmGuardado = ventaMuebleService.guardar(vm);
            detallesGuardados.add(vmGuardado);
            total += vmGuardado.getSubtotal();
        }

        // 3) Actualizamos la venta con la lista completa y el total calculado
        ventaGuardada.setVentaMuebles(detallesGuardados);
        ventaGuardada.setTotal(total);

        // 4) Guardamos nuevamente la venta para actualizar total y detalles
        return ventaRepository.save(ventaGuardada);
    }
}
