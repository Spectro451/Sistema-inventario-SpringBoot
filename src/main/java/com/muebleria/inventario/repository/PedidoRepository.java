package com.muebleria.inventario.repository;

import com.muebleria.inventario.entidad.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
