package com.colors.savd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.VentaDetalle;
import com.colors.savd.repository.projection.ResumenVentaAgg;

@Repository
public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

  List<VentaDetalle> findByVenta_Id(Long ventaId);

  /**
   * Totales por venta (unidades e importe), útil para recalcular/validar total de cabecera.
   * Retorna [Long ventaId, Long unidades, Double importe]
   */
  @Query("""
    SELECT vd.venta.id AS ventaId, 
        SUM(vd.cantidad) AS unidades, 
        SUM(vd.importe) AS importe 
      FROM VentaDetalle vd 
    WHERE vd.venta.id = :ventaId
  GROUP BY vd.venta.id
  """)
  List<ResumenVentaAgg> resumenPorVenta(@Param("ventaId") Long ventaId);
}