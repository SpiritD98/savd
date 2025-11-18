package com.colors.savd.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.colors.savd.exception.BusinessException;
import com.colors.savd.exception.GlobalExceptionHandler;
import com.colors.savd.service.ReporteService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteService reporteService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Test
    @DisplayName("GET /api/reportes/kpis/producto debe responder 200 y JSON (lista) cuando el servicio retorna OK")
    void kpiProductoMensual_debeRetornar200() throws Exception {
        // Arrange
        given(reporteService.kpiProductoMensual(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(/* puedes dejarlo vacío o poner DTOs */));

        String desde = LocalDateTime.of(2025, 1, 1, 0, 0).format(ISO);
        String hasta = LocalDateTime.of(2025, 1, 31, 23, 59).format(ISO);

        // Act + Assert
        mockMvc.perform(get("/api/reportes/kpis/producto")
                    .param("desde", desde)
                    .param("hasta", hasta)
                    .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/reportes/kpis/producto/total debe responder 200 y JSON (lista) cuando el servicio retorna OK")
    void kpiProductoTotal_debeRetornar200() throws Exception {
        // Arrange
        given(reporteService.kpiPorProducto(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(/* KpiProductoDTO.builder()...build() si quieres */));

        String desde = LocalDateTime.of(2025, 2, 1, 0, 0).format(ISO);
        String hasta = LocalDateTime.of(2025, 2, 28, 23, 59).format(ISO);

        // Act + Assert
        mockMvc.perform(get("/api/reportes/kpis/producto/total")
                    .param("desde", desde)
                    .param("hasta", hasta)
                    .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/reportes/kpis/producto debe responder 400 cuando el servicio lanza BusinessException (p.ej. rango inválido)")
    void kpiProductoMensual_debeRetornar400_siBusinessException() throws Exception {
        // Arrange
        given(reporteService.kpiProductoMensual(any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new BusinessException("Rango inválido"));

        // (fuerzo un 'desde' mayor que 'hasta' sólo como ejemplo de entrada)
        String desde = LocalDateTime.of(2025, 3, 31, 23, 59).format(ISO);
        String hasta = LocalDateTime.of(2025, 3, 1, 0, 0).format(ISO);

        // Act + Assert
        mockMvc.perform(get("/api/reportes/kpis/producto")
                    .param("desde", desde)
                    .param("hasta", hasta)
                    .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.mensaje").value("Rango inválido"))
               .andExpect(jsonPath("$.code").value("BUSINESS")); // según tu GlobalExceptionHandler
    }
}