package com.colors.savd.service;

import com.colors.savd.dto.MovimientoInventarioDTO;

public interface InventarioService {
    Long registrarStockInicial(MovimientoInventarioDTO dto, Long usuarioId);
    Long registrarIngreso(MovimientoInventarioDTO dto, Long usuarioId);
}
