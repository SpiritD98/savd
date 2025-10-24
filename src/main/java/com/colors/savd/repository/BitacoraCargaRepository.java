package com.colors.savd.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.BitacoraCarga;
import com.colors.savd.model.enums.TipoCarga;

@Repository
public interface BitacoraCargaRepository extends JpaRepository<BitacoraCarga, Long>{

    List<BitacoraCarga> findByUsuario_IdOrderByFechaHoraDesc(Long usuarioId);

    List<BitacoraCarga> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);

    List<BitacoraCarga> findByTipoCargaOrderByFechaHoraDesc(TipoCarga tipoCarga);

}
