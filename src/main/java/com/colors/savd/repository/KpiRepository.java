package com.colors.savd.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.model.VentaDetalle;
import com.colors.savd.repository.projection.KpiAggCategoria;
import com.colors.savd.repository.projection.KpiAggCategoriaTotal;
import com.colors.savd.repository.projection.KpiAggProducto;
import com.colors.savd.repository.projection.KpiAggProductoTotal;
import com.colors.savd.repository.projection.KpiAggSku;
import com.colors.savd.repository.projection.KpiAggSkuTotal;

@Repository
@Transactional(readOnly = true)
public interface KpiRepository extends JpaRepository<VentaDetalle, Long>{
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

    @Query(value = """
        SELECT 
            YEAR(v.fecha_hora)       AS anio,
            MONTH(v.fecha_hora)      AS mes,
            p.id                     AS productoId,
            p.nombre                 AS producto,
            SUM(vd.cantidad)         AS unidades,
            SUM(vd.importe)          AS ingresos
        FROM venta_detalle vd
        JOIN venta v               ON vd.venta_id = v.id
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
        GROUP BY anio, mes, productoId, producto
        ORDER BY anio, mes, ingresos DESC
        """, nativeQuery = true)
    List<KpiAggProducto> kpiProductoMensual(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("canalId") Long canalId,
            @Param("temporadaId") Long temporadaId,
            @Param("categoriaId") Long categoriaId,
            @Param("tallaId") Long tallaId,
            @Param("colorId") Long colorId
    );

    @Query(value = """
        SELECT 
            YEAR(v.fecha_hora)       AS anio,
            MONTH(v.fecha_hora)      AS mes,
            vs.id                    AS skuId,
            vs.sku                   AS sku,
            p.nombre                 AS producto,
            ta.codigo                AS talla,
            co.nombre                AS color,
            SUM(vd.cantidad)         AS unidades,
            SUM(vd.importe)          AS ingresos
        FROM venta_detalle vd
        JOIN venta v               ON vd.venta_id = v.id
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
        GROUP BY anio, mes, skuId, sku, producto, talla, color
        ORDER BY anio, mes, ingresos DESC
        """, nativeQuery = true)
    List<KpiAggSku> kpiSkuMensual(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("canalId") Long canalId,
            @Param("temporadaId") Long temporadaId,
            @Param("categoriaId") Long categoriaId,
            @Param("tallaId") Long tallaId,
            @Param("colorId") Long colorId
    );

    @Query(value = """
        SELECT 
            c.id       AS categoriaId,
            c.nombre   AS categoria,
            SUM(vd.cantidad) AS unidades,
            SUM(vd.importe)  AS ingresos
        FROM venta_detalle vd
        JOIN venta v             ON vd.venta_id = v.id
                                AND v.estado = 'ACTIVA'
                                AND v.fecha_hora BETWEEN :desde AND :hasta
        JOIN variante_sku vs     ON vd.sku_id = vs.id
        JOIN producto p          ON vs.producto_id = p.id
        JOIN categoria c         ON p.categoria_id = c.id
        LEFT JOIN temporada t    ON v.temporada_id = t.id
        LEFT JOIN canal_venta cv ON v.canal_id = cv.id
        LEFT JOIN talla ta       ON vs.talla_id = ta.id
        LEFT JOIN color co       ON vs.color_id = co.id
        WHERE (:canalId     IS NULL OR v.canal_id     = :canalId)
        AND (:temporadaId IS NULL OR v.temporada_id = :temporadaId)
        AND (:categoriaId IS NULL OR c.id           = :categoriaId)
        AND (:tallaId     IS NULL OR ta.id          = :tallaId)
        AND (:colorId     IS NULL OR co.id          = :colorId)
        GROUP BY c.id, c.nombre
        ORDER BY ingresos DESC
        """, nativeQuery = true)
    List<KpiAggCategoriaTotal> kpiCategoriaTotal(
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta,
            @Param("canalId") Long canalId,
            @Param("temporadaId") Long temporadaId,
            @Param("categoriaId") Long categoriaId,
            @Param("tallaId") Long tallaId,
            @Param("colorId") Long colorId
    );

    @Query(value = """
        SELECT 
            p.id                   AS productoId,
            p.nombre               AS producto,
            SUM(vd.cantidad)       AS unidades,
            SUM(vd.importe)        AS ingresos
        FROM venta_detalle vd
        JOIN venta v             ON vd.venta_id = v.id
                                AND v.estado = 'ACTIVA'
                                AND v.fecha_hora BETWEEN :desde AND :hasta
        JOIN variante_sku vs     ON vd.sku_id = vs.id
        JOIN producto p          ON vs.producto_id = p.id
        JOIN categoria c         ON p.categoria_id = c.id
        LEFT JOIN temporada t    ON v.temporada_id = t.id
        LEFT JOIN canal_venta cv ON v.canal_id = cv.id
        LEFT JOIN talla ta       ON vs.talla_id = ta.id
        LEFT JOIN color co       ON vs.color_id = co.id
        WHERE (:canalId     IS NULL OR v.canal_id     = :canalId)
        AND (:temporadaId IS NULL OR v.temporada_id = :temporadaId)
        AND (:categoriaId IS NULL OR c.id           = :categoriaId)
        AND (:tallaId     IS NULL OR ta.id          = :tallaId)
        AND (:colorId     IS NULL OR co.id          = :colorId)
        GROUP BY p.id, p.nombre
        ORDER BY ingresos DESC
        """, nativeQuery = true)
    List<KpiAggProductoTotal> kpiProductoTotal(
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta,
            @Param("canalId") Long canalId,
            @Param("temporadaId") Long temporadaId,
            @Param("categoriaId") Long categoriaId,
            @Param("tallaId") Long tallaId,
            @Param("colorId") Long colorId
    );

    @Query(value = """
        SELECT 
            vs.id                  AS skuId,
            vs.sku                 AS sku,
            p.nombre               AS producto,
            ta.codigo              AS talla,
            co.nombre              AS color,
            SUM(vd.cantidad)       AS unidades,
            SUM(vd.importe)        AS ingresos
        FROM venta_detalle vd
        JOIN venta v             ON vd.venta_id = v.id
                                AND v.estado = 'ACTIVA'
                                AND v.fecha_hora BETWEEN :desde AND :hasta
        JOIN variante_sku vs     ON vd.sku_id = vs.id
        JOIN producto p          ON vs.producto_id = p.id
        JOIN categoria c         ON p.categoria_id = c.id
        LEFT JOIN temporada t    ON v.temporada_id = t.id
        LEFT JOIN canal_venta cv ON v.canal_id = cv.id
        LEFT JOIN talla ta       ON vs.talla_id = ta.id
        LEFT JOIN color co       ON vs.color_id = co.id
        WHERE (:canalId     IS NULL OR v.canal_id     = :canalId)
        AND (:temporadaId IS NULL OR v.temporada_id = :temporadaId)
        AND (:categoriaId IS NULL OR c.id           = :categoriaId)
        AND (:tallaId     IS NULL OR ta.id          = :tallaId)
        AND (:colorId     IS NULL OR co.id          = :colorId)
        GROUP BY vs.id, vs.sku, p.nombre, ta.codigo, co.nombre
        ORDER BY ingresos DESC
        """, nativeQuery = true)
    List<KpiAggSkuTotal> kpiSkuTotal(
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta,
            @Param("canalId") Long canalId,
            @Param("temporadaId") Long temporadaId,
            @Param("categoriaId") Long categoriaId,
            @Param("tallaId") Long tallaId,
            @Param("colorId") Long colorId
    );
}
