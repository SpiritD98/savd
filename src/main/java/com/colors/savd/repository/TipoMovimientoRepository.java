package com.colors.savd.repository;

import com.colors.savd.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Long> {
  Optional<TipoMovimiento> findByCodigo(String codigo);}
