package com.colors.savd.controller;

import com.colors.savd.dto.LineaVentaDTO;
import com.colors.savd.dto.VentaManualDTO;
import com.colors.savd.model.*;
import com.colors.savd.model.enums.EstadoVenta;
import com.colors.savd.repository.VentaDetalleRepository;
import com.colors.savd.repository.VentaRepository;
import com.colors.savd.service.VentaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(VentaController.class)
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VentaService ventaService;

    @MockBean
    private VentaRepository ventaRepo;

    @MockBean
    private VentaDetalleRepository ventaDetRepo;

    @Test
    @WithMockUser(roles = {"ADMIN"}) // satisface las reglas de SecurityConfig
    void crearVentaManual_deberiaRetornar201ConVentaRespuesta() throws Exception {
        // ---------- Arrange ----------
        Long usuarioId = 1L;
        Long canalId = 10L;
        Long skuId = 100L;
        Long ventaId = 999L;

        LocalDateTime fecha = LocalDateTime.of(2025, 1, 1, 10, 0);

        LineaVentaDTO item = LineaVentaDTO.builder()
                .skuId(skuId)
                .cantidad(2)
                .precioUnitario(new BigDecimal("50.00"))
                .build();

        VentaManualDTO dto = VentaManualDTO.builder()
                .fechaHora(fecha)
                .canalId(canalId)
                .referenciaOrigen("TICKET-001")
                .items(List.of(item))
                .build();

        // Cuando el service registra, devuelve el ID de la venta
        when(ventaService.registrarVentaManual(any(VentaManualDTO.class), eq(usuarioId)))
                .thenReturn(ventaId);

        // Simulamos la venta guardada en BD
        CanalVenta canal = new CanalVenta();
        canal.setId(canalId);
        canal.setCodigo("FISICO");

        Venta venta = new Venta();
        venta.setId(ventaId);
        venta.setFechaHora(fecha);
        venta.setCanal(canal);
        venta.setReferenciaOrigen("TICKET-001");
        venta.setEstado(EstadoVenta.ACTIVA);
        venta.setTotal(new BigDecimal("100.00"));

        when(ventaRepo.findById(ventaId)).thenReturn(Optional.of(venta));

        // Detalle de venta con SKU
        Producto prod = new Producto();
        prod.setNombre("Producto Test");

        Talla talla = new Talla();
        talla.setCodigo("M");

        Color color = new Color();
        color.setNombre("Negro");

        VarianteSku sku = new VarianteSku();
        sku.setId(skuId);
        sku.setSku("SKU-TEST");
        sku.setProducto(prod);
        sku.setTalla(talla);
        sku.setColor(color);

        VentaDetalle det = new VentaDetalle();
        det.setSku(sku);
        det.setCantidad(2);
        det.setPrecioUnitario(new BigDecimal("50.00"));
        det.setImporte(new BigDecimal("100.00"));

        when(ventaDetRepo.findByVenta_Id(ventaId)).thenReturn(List.of(det));

        String jsonBody = objectMapper.writeValueAsString(dto);

        // ---------- Act & Assert ----------
        mockMvc.perform(
                        post("/api/ventas")
                                .with(csrf()) // simulamos token CSRF valido
                                .param("usuarioId", usuarioId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.ventaId").value(ventaId))
                .andExpect(jsonPath("$.canalCodigo").value("FISICO"))
                .andExpect(jsonPath("$.referenciaOrigen").value("TICKET-001"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.detalles[0].sku").value("SKU-TEST"))
                .andExpect(jsonPath("$.detalles[0].producto").value("Producto Test"))
                .andExpect(jsonPath("$.detalles[0].talla").value("M"))
                .andExpect(jsonPath("$.detalles[0].color").value("Negro"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void obtenerVenta_deberiaRetornar200ConVentaRespuesta() throws Exception {
        Long ventaId = 500L;
        Long canalId = 20L;
        Long skuId = 200L;

        LocalDateTime fecha = LocalDateTime.of(2025, 2, 1, 15, 30);

        CanalVenta canal = new CanalVenta();
        canal.setId(canalId);
        canal.setCodigo("ONLINE");

        Venta venta = new Venta();
        venta.setId(ventaId);
        venta.setFechaHora(fecha);
        venta.setCanal(canal);
        venta.setReferenciaOrigen("ORD-123");
        venta.setEstado(EstadoVenta.ACTIVA);
        venta.setTotal(new BigDecimal("200.00"));

        when(ventaRepo.findById(ventaId)).thenReturn(Optional.of(venta));

        Producto prod = new Producto();
        prod.setNombre("Zapatilla X");

        Talla talla = new Talla();
        talla.setCodigo("42");

        Color color = new Color();
        color.setNombre("Rojo");

        VarianteSku sku = new VarianteSku();
        sku.setId(skuId);
        sku.setSku("ZAP-42-ROJO");
        sku.setProducto(prod);
        sku.setTalla(talla);
        sku.setColor(color);

        VentaDetalle det = new VentaDetalle();
        det.setSku(sku);
        det.setCantidad(1);
        det.setPrecioUnitario(new BigDecimal("200.00"));
        det.setImporte(new BigDecimal("200.00"));

        when(ventaDetRepo.findByVenta_Id(ventaId)).thenReturn(List.of(det));

        mockMvc.perform(
                        get("/api/ventas/{id}", ventaId)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventaId").value(ventaId))
                .andExpect(jsonPath("$.canalCodigo").value("ONLINE"))
                .andExpect(jsonPath("$.referenciaOrigen").value("ORD-123"))
                .andExpect(jsonPath("$.detalles[0].sku").value("ZAP-42-ROJO"))
                .andExpect(jsonPath("$.detalles[0].producto").value("Zapatilla X"))
                .andExpect(jsonPath("$.detalles[0].talla").value("42"))
                .andExpect(jsonPath("$.detalles[0].color").value("Rojo"));
    }
}
