package com.colors.savd.repository;

import com.colors.savd.model.CanalVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CanalVentaRepository extends JpaRepository<CanalVenta, Long> {
    Optional<CanalVenta> findByCodigo(String codigo);
}