package com.colors.savd.service.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.ImportResultadoDTO;
import com.colors.savd.model.*;
import com.colors.savd.model.enums.TipoCarga;
import com.colors.savd.repository.*;
import com.colors.savd.service.ImportacionService;
import com.colors.savd.util.ExcelUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacionServiceImpl implements ImportacionService {

  private final BitacoraCargaRepository bitacoraRepo;
  private final BitacoraErrorRepository bitErrorRepo;
  private final VarianteSkuRepository skuRepo;
  private final VentaRepository ventaRepo;
  private final VentaDetalleRepository ventaDetRepo;
  private final KardexRepository kardexRepo;
  private final ExcelUtil excelUtil;
  private final UsuarioRepository usuarioRepo;

  @Override
  @Transactional
  public ImportResultadoDTO importarVentasExcel(InputStream in, String nombreArchivo, Long usuarioId) {
    // 1) Crear bitácora
    BitacoraCarga bit = new BitacoraCarga();
    bit.setFechaHora(LocalDateTime.now());
    bit.setUsuario(usuarioRepo); // o cargar de repo si prefieres
    bit.setTipoCarga(TipoCarga.VENTAS);
    bit.setArchivoNombre(nombreArchivo);
    bit.setFilasOk(0);
    bit.setFilasError(0);
    bit = bitacoraRepo.save(bit);

    int ok = 0, err = 0;
    List<String> erroresMuestra = new ArrayList<>();

    try (Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet = wb.getSheetAt(0);
      // TODO: validar encabezados esperados (Fecha, Canal, Ref, SKU, Cantidad, PrecioUnitario, PrecioLista)
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        try {
          // TODO: parsear columnas → validar nulos/vacíos
          // Ejemplo (ajusta índices):
          String sku = excelUtil.leerString(row.getCell(3));
          Integer cantidad = excelUtil.leerEntero(row.getCell(4));
          // ... leer más columnas

          if (StringUtils.isBlank(sku) || cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Fila con datos obligatorios vacíos o inválidos");
          }

          // TODO: buscar VarianteSku, crear Venta (o agrupar por cabecera), crear VentaDetalle
          // TODO: registrar movimiento en Kardex con idempotency si corresponde

          ok++;
        } catch (Exception e) {
          err++;
          String msg = "Fila " + (i + 1) + ": " + e.getMessage();
          erroresMuestra.add(msg);
          BitacoraError be = new BitacoraError();
          be.setBitacora(bit);
          be.setFilaOrigen(i + 1);
          be.setCampo("GENERAL");
          be.setMensajeError(e.getMessage());
          be.setFechaHoraRegistro(LocalDateTime.now());
          bitErrorRepo.save(be);
          log.error("Error importando fila {}: {}", i + 1, e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error leyendo Excel: " + e.getMessage(), e);
    }

    bit.setFilasOk(ok);
    bit.setFilasError(err);
    bitacoraRepo.save(bit);

    return ImportResultadoDTO.builder()
        .bitacoraId(bit.getId())
        .filasOk(ok)
        .filasError(err)
        .erroresMuestra(erroresMuestra.size() > 10 ? erroresMuestra.subList(0, 10) : erroresMuestra)
        .build();
  }
}
