package com.colors.savd.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.Venta;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>{

    List<Venta> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Venta> findByFechaHoraBetweenAndCanal_Id(LocalDateTime desde, LocalDateTime hasta, Long canalId);

    Boolean existsByFechaHoraAndCanal_IdAndReferenciaOrigen(LocalDateTime fechaHora, Long canalId, String referenciaOrigen);

    //Agregacion para top 15 por SKU en un rango de fechas.
    @Query("""
        SELECT vd.sku.id AS skuId,
            SUM(vd.cantidad) AS unidades,
            SUM(vd.importe) AS ingresos 
        FROM VentaDetalle vd 
        JOIN vd.venta v 
        WHERE v.fechaHora BETWEEN :desde AND :hasta 
            AND (:canalId IS NULL OR v.canal.id = :canalId) 
            AND v.estado = com.colors.savd.model.enums.EstadoVenta.ACTIVA 
        GROUP BY vd.sku.id 
        ORDER BY SUM(vd.cantidad) DESC
        """)
    List<Object[]> top15ByRango(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta,
                                @Param("canalId") Long canalId);
}
