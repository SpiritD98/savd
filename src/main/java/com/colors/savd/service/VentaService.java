package com.colors.savd.service;

import com.colors.savd.dto.VentaManualDTO;

public interface VentaService {
    Long registrarVentaManual(VentaManualDTO dto, Long usuarioId);
    void anularVenta(Long ventaId, Long usuarioId, String motivo);
}
