package com.colors.savd.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class VentaRespuestaDTO {

    private Long ventaId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;

    private String canalCodigo;      // FISICO / ONLINE
    private String referenciaOrigen; // puede venir null si no es FISICO
    private String estado;           // ACTIVA / ANULADA

    private BigDecimal total;

    @Builder.Default
    private List<LineaVentaRespDTO> detalles = new ArrayList<>();
}
