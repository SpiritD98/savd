package com.colors.savd.service.impl;

import com.colors.savd.dto.ImportResultadoDTO;
import com.colors.savd.model.*;
import com.colors.savd.repository.*;
import com.colors.savd.util.ExcelUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportacionServiceImplTest {

  @Mock BitacoraCargaRepository bitacoraRepo;
  @Mock BitacoraErrorRepository bitErrorRepo;
  @Mock VarianteSkuRepository skuRepo;
  @Mock VentaRepository ventaRepo;
  @Mock VentaDetalleRepository ventaDetRepo;
  @Mock KardexRepository kardexRepo; // <— nombre correcto
  @Mock CanalVentaRepository canalRepo;
  @Mock TipoMovimientoRepository tipoMovRepo;
  @Mock UsuarioRepository usuarioRepo;

  // Usar métodos reales de ExcelUtil para que el parseo funcione
  @Spy ExcelUtil excelUtil = new ExcelUtil();

  @InjectMocks ImportacionServiceImpl service;

  @Test
  void importarVentasExcel_ok_agrupaPorCabecera() throws Exception {
    // ========= 1) XLSX en memoria =========
    var wb = new XSSFWorkbook();
    var sheet = wb.createSheet("Ventas");
    int r = 0;

    var header = sheet.createRow(r++);
    header.createCell(0).setCellValue("FechaHora");
    header.createCell(1).setCellValue("Canal");
    header.createCell(2).setCellValue("Referencia");
    header.createCell(3).setCellValue("SKU");
    header.createCell(4).setCellValue("Cantidad");
    header.createCell(5).setCellValue("PrecioUnitario");
    header.createCell(6).setCellValue("PrecioLista");

    // Fila 1
    var row1 = sheet.createRow(r++);
    row1.createCell(0).setCellValue("2025-01-10 10:00:00");
    row1.createCell(1).setCellValue("ONLINE");
    row1.createCell(2).setCellValue("WEB-1001");
    row1.createCell(3).setCellValue("SKU-001");
    row1.createCell(4).setCellValue(2);
    row1.createCell(5).setCellValue(30.0);
    row1.createCell(6).setCellValue(35.0);

    // Fila 2 (misma cabecera -> debe agruparse con la fila 1)
    var row2 = sheet.createRow(r++);
    row2.createCell(0).setCellValue("2025-01-10 10:00:00");
    row2.createCell(1).setCellValue("ONLINE");
    row2.createCell(2).setCellValue("WEB-1001");
    row2.createCell(3).setCellValue("SKU-001");
    row2.createCell(4).setCellValue(1);
    row2.createCell(5).setCellValue(30.0);
    row2.createCell(6).setCellValue(35.0);

    var bos = new ByteArrayOutputStream();
    wb.write(bos);
    wb.close();
    var in = new ByteArrayInputStream(bos.toByteArray());

    // ========= 2) Stubs que SÍ se usan =========
    when(bitacoraRepo.save(any())).thenAnswer(inv -> {
      BitacoraCarga b = inv.getArgument(0);
      b.setId(1L);
      return b;
    });

    // usuario
    var usr = new Usuario(); usr.setId(99L);
    when(usuarioRepo.getReferenceById(99L)).thenReturn(usr);

    // canal ONLINE
    var canal = new CanalVenta(); canal.setId(2L); canal.setCodigo("ONLINE");
    when(canalRepo.findByCodigo("ONLINE")).thenReturn(Optional.of(canal));
    when(canalRepo.findById(2L)).thenReturn(Optional.of(canal));

    // tipo movimiento VENTA
    var tm = new TipoMovimiento(); tm.setId(5L); tm.setCodigo("VENTA"); tm.setSignoDefault(-1);
    when(tipoMovRepo.findByCodigo("VENTA")).thenReturn(Optional.of(tm));

    // SKU
    var varSku = new VarianteSku(); varSku.setId(10L); varSku.setSku("SKU-001"); varSku.setPrecioLista(new BigDecimal("35"));
    when(skuRepo.findBySku("SKU-001")).thenReturn(Optional.of(varSku));
    when(skuRepo.findById(10L)).thenReturn(Optional.of(varSku));

    // venta save: asigna id a cabecera y devuelve misma instancia
    when(ventaRepo.save(any(Venta.class))).thenAnswer(inv -> {
      Venta v = inv.getArgument(0);
      if (v.getId() == null) v.setId(100L);
      return v;
    });

    // detalle save: asigna id
    when(ventaDetRepo.save(any(VentaDetalle.class))).thenAnswer(inv -> {
      VentaDetalle d = inv.getArgument(0);
      if (d.getId() == null) d.setId(1000L + (long)(Math.random()*100));
      return d;
    });

    // ========= 3) Ejecutar =========
    ImportResultadoDTO res = service.importarVentasExcel(in, "ventas.xlsx", 99L);

    // ========= 4) Aserciones =========
    assertNotNull(res);
    assertEquals(2, res.getFilasOk());
    assertEquals(0, res.getFilasError());

    // Se debe haber creado una venta con 2 detalles y 2 movimientos en kardex
    verify(ventaRepo, atLeastOnce()).save(any(Venta.class));      // crear + actualizar total
    verify(ventaDetRepo, times(2)).save(any(VentaDetalle.class)); // 2 items
    verify(kardexRepo, times(2)).save(any(KardexMovimiento.class));
  }
}

