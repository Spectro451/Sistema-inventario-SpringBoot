package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Mueble;
import com.muebleria.inventario.entidad.VentaMueble;
import com.muebleria.inventario.repository.MuebleRepository;
import com.muebleria.inventario.repository.VentaMuebleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VentaMuebleService {

    @Autowired
    VentaMuebleRepository ventaMuebleRepository;

    @Autowired
    MuebleRepository muebleRepository;

   public List<VentaMueble> findAll() {
       return ventaMuebleRepository.findAll();
   }

   public Optional<VentaMueble> getById(Long id) {
       return ventaMuebleRepository.findById(id);
   }

   public List<VentaMueble> obtenerPorMuebleId(Long muebleId) {
       return ventaMuebleRepository.findByMuebleId(muebleId);
   }

    public List<VentaMueble> obtenerPorVentaId(Long ventaId) {
        return ventaMuebleRepository.findByVentaId(ventaId);
    }

    public VentaMueble guardar(VentaMueble vm) {
        Long muebleId = vm.getMueble().getId();
        Long cantidad = vm.getCantidad();

        Mueble mueble = muebleRepository.findById(muebleId)
                .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + muebleId));

        if (mueble.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente del mueble: " + mueble.getNombre());
        }

        mueble.setStock(mueble.getStock() - cantidad);
        muebleRepository.save(mueble);

        Long precioUnitario = mueble.getPrecioVenta();
        Long subTotal = precioUnitario * cantidad;

        System.out.println("precioUnitario calculado: " + precioUnitario);
        System.out.println("subtotal calculado: " + subTotal);

        vm.setPrecioUnitario(precioUnitario);
        vm.setSubtotal(subTotal);

        vm.setMueble(mueble);

        return ventaMuebleRepository.save(vm);
    }

    public void deleteById(Long id) {
        if (!ventaMuebleRepository.existsById(id)) {
            throw new RuntimeException("VentaMueble con id " + id + " no existe.");
        }
        ventaMuebleRepository.deleteById(id);
    }
}
