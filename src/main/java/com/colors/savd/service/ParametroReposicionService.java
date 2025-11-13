package com.colors.savd.service;

import java.util.List;

import com.colors.savd.dto.ParametroReposicionDTO;

public interface ParametroReposicionService {
    ParametroReposicionDTO crear(ParametroReposicionDTO dto);
    ParametroReposicionDTO actualizar(Long id, ParametroReposicionDTO dto);
    void eliminar(Long id);
    ParametroReposicionDTO obtener(Long id);
    List<ParametroReposicionDTO> listar();
}
