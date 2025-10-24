package com.colors.savd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.VentaDetalle;

@Repository
public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

  List<VentaDetalle> findByVenta_Id(Long ventaId);

  /**
   * Totales por venta (unidades e importe), útil para recalcular/validar total de cabecera.
   * Retorna [Long ventaId, Long unidades, Double importe]
   */
  @Query("""
         SELECT vd.venta.id, SUM(vd.cantidad), SUM(vd.importe)
           FROM VentaDetalle vd
          WHERE vd.venta.id = :ventaId
          GROUP BY vd.venta.id
         """)
  List<Object[]> resumenPorVenta(@Param("ventaId") Long ventaId);
}