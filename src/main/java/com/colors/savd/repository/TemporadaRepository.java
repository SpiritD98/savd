package com.colors.savd.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.Temporada;

@Repository
public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

  /**
   * Devuelve la mejor coincidencia ACTIVA que contenga la fecha dada:
   * - Prioridad más alta primero
   * - A igual prioridad, intervalo más corto primero
   */
  @Query(
      value = """
              SELECT * 
              FROM temporada t
              WHERE t.estado_negocio = 'ACTIVA'
                AND t.fecha_inicio <= :fecha
                AND t.fecha_fin   >= :fecha
              ORDER BY t.prioridad DESC,
                       DATEDIFF(t.fecha_fin, t.fecha_inicio) ASC
              LIMIT 1
              """,
      nativeQuery = true
  )
  Optional<Temporada> findActivaQueContenga(@Param("fecha") LocalDate fecha);

  /**
   * Lista todas las temporadas ACTIVAS que contengan la fecha (por si quieres inspeccionar o resolver empates manualmente).
   */
  @Query(
      value = """
              SELECT * 
              FROM temporada t
              WHERE t.estado_negocio = 'ACTIVA'
                AND t.fecha_inicio <= :fecha
                AND t.fecha_fin   >= :fecha
              ORDER BY t.prioridad DESC,
                       DATEDIFF(t.fecha_fin, t.fecha_inicio) ASC
              """,
      nativeQuery = true
  )
  List<Temporada> findActivasQueContengan(@Param("fecha") LocalDate fecha);

  /**
   * Recupera una temporada ACTIVA por id (útil para el modo FIJAR).
   */
  @Query(
      value = """
              SELECT *
              FROM temporada t
              WHERE t.id = :id
                AND t.estado_negocio = 'ACTIVA'
              """,
      nativeQuery = true
  )
  Optional<Temporada> findActivaById(@Param("id") Long id);
}
