package com.colors.savd.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colors.savd.repository.projection.KpiAggCategoria;

public interface KpiRepository {
    @Query(value = """
            SELECT 
                YEAR(v.fecha_hora)       AS anio,
                MONTH(v.fecha_hora)      AS mes,
                c.id                     AS categoriaId,
                c.nombre                 AS categoria,
                SUM(vd.cantidad)         AS unidades,
                SUM(vd.importe)          AS ingresos
            FROM venta_detalle vd
            JOIN venta v                ON vd.venta_id = v.id
                                        AND v.estado = 'ACTIVA'
                                        AND v.fecha_hora BETWEEN :desde AND :hasta
            JOIN variante_sku vs       ON vd.sku_id = vs.id
            JOIN producto p            ON vs.producto_id = p.id
            JOIN categoria c           ON p.categoria_id = c.id
            LEFT JOIN temporada t      ON v.temporada_id = t.id
            LEFT JOIN canal_venta cv   ON v.canal_id = cv.id
            LEFT JOIN talla ta         ON vs.talla_id = ta.id
            LEFT JOIN color co         ON vs.color_id = co.id
            WHERE (:canalId     IS NULL OR v.canal_id     = :canalId)
                AND (:temporadaId IS NULL OR v.temporada_id = :temporadaId)
                AND (:categoriaId IS NULL OR c.id           = :categoriaId)
                AND (:tallaId     IS NULL OR ta.id          = :tallaId)
                AND (:colorId     IS NULL OR co.id          = :colorId)
            GROUP BY anio, mes, categoriaId, categoria
            ORDER BY anio, mes, ingresos DESC                                
            """, nativeQuery = true)
    List<KpiAggCategoria> kpiCategoriaMensual(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("canalId") Long canalId,
        @Param("temporadaId") Long temporadaId,
        @Param("categoriaId") Long categoriaId,
        @Param("tallaId") Long tallaId,
        @Param("colorId") Long colorId
    );
}
