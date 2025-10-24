package com.colors.savd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.CanalVenta;

@Repository
public interface CanalVentaRepository extends JpaRepository<CanalVenta, Long> {
    Optional<CanalVenta> findByCodigo(String codigo);
}