package com.colors.savd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.Temporada;

@Repository
public interface TemporadaRepository extends JpaRepository<Temporada, Long> {
}
