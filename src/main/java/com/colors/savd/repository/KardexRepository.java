package com.colors.savd.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.KardexMovimiento;

@Repository
public interface KardexRepository extends JpaRepository<KardexMovimiento, Long> {

  List<KardexMovimiento> findBySku_IdOrderByFechaHoraDesc(Long skuId);

  /**
   * Stock actual por SKU = SUM(signo * cantidad) hasta 'corte' (o todo si corte es null → usar NOW en service).
   * Retorna filas: [Long skuId, Long stock]
   */
  @Query("""
         SELECT k.sku.id AS skuId,
                SUM(k.signo * k.cantidad) AS stock
         FROM KardexMovimiento k
         WHERE (:corte IS NULL OR k.fechaHora <= :corte)
           AND k.sku.id IN :skuIds
         GROUP BY k.sku.id
         """)
  List<Object[]> stockPorSkuHasta(@Param("skuIds") Collection<Long> skuIds,
                                  @Param("corte") LocalDateTime corte);

  /**
   * Movimiento agregado por SKU en un rango (útil para cobertura promedio si usas ventana).
   * Retorna filas: [Long skuId, Long unidadesVendidas]
   */
  @Query("""
         SELECT k.sku.id AS skuId,
                SUM(CASE WHEN k.signo = -1 THEN k.cantidad ELSE 0 END) AS unidadesVendidas
         FROM KardexMovimiento k
         WHERE k.fechaHora BETWEEN :desde AND :hasta
           AND k.sku.id IN :skuIds
         GROUP BY k.sku.id
         """)
  List<Object[]> ventasPorSkuEnRango(@Param("skuIds") Collection<Long> skuIds,
                                     @Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta);
}
