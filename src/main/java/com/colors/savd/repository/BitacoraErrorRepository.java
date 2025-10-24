package com.colors.savd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.BitacoraError;

@Repository
public interface BitacoraErrorRepository extends JpaRepository<BitacoraError, Long> {

  List<BitacoraError> findByBitacora_IdOrderByFechaHoraRegistroAsc(Long bitacoraId);

  /**
   * Conteo de errores por campo en una bitácora (útil para mostrar "top errores").
   * Retorna [String campo, Long cantidad]
   */
  @Query("""
         SELECT b.campo, COUNT(b.id)
           FROM BitacoraError b
          WHERE b.bitacora.id = :bitacoraId
          GROUP BY b.campo
          ORDER BY COUNT(b.id) DESC
         """)
  List<Object[]> contarErroresPorCampo(@Param("bitacoraId") Long bitacoraId);
}
