package com.colors.savd.util;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.colors.savd.dto.AlertaStockDTO;
import com.colors.savd.dto.TopProductoDTO;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExcelUtil {
    private static final DataFormatter DF = new DataFormatter(Locale.getDefault());

    //Formatos comunes de fecha/hora cuando vienen como texto
    private static final List<DateTimeFormatter> TXT_DATE_TIME_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    );

    // ===== Lectura segura de celdas =====
    public String leerString(Cell cell){
        if (cell == null) return null;
        try{
            String val = DF.formatCellValue(cell);
            return StringUtils.isBlank(val) ? null : val.trim();
        } catch (Exception e){
            log.warn("Error leyendo celda String: {}", e.getMessage());
            return null;
        }
    }

    public Integer leerEntero(Cell cell){
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()){
                case STRING -> parseEnteroSeguro(cell.getStringCellValue());
                case NUMERIC -> (int) Math.round(cell.getNumericCellValue());
                case FORMULA -> {
                    try{
                        yield (int) Math.round(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        yield parseEnteroSeguro(DF.formatCellValue(cell));
                    }
                }
                default -> parseEnteroSeguro(DF.formatCellValue(cell));
            };
        } catch (Exception e) {
            log.debug("leerEntero error: {}", e.getMessage());
            return null;
        }   
    }
    
    public BigDecimal leerDecimal(Cell cell){
        if (cell == null) return null;
        try{
            return switch (cell.getCellType()){
                case STRING -> parseDecimalSeguro(cell.getStringCellValue());
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case FORMULA -> {
                    try {
                        yield BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        yield parseDecimalSeguro(DF.formatCellValue(cell));
                    }
                }
                default -> parseDecimalSeguro(DF.formatCellValue(cell));
            };
        } catch (Exception e){
            log.debug("leerDecimal error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lee una fecha/hora. Acepta:
     * - NUMERIC con formato fecha Excel (DateUtil.isCellDateFormatted)
     * - STRING con varios patrones comunes
    */
    public LocalDateTime leerFechaHora(Cell cell){
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date d = cell.getDateCellValue();
                    return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
                } else {
                    return null;
                }
            }
            String txt = DF.formatCellValue(cell);
            if (StringUtils.isBlank(txt)) return null;
            txt = txt.trim();

            //Intentar con patrones conocidos
            for (DateTimeFormatter fmt : TXT_DATE_TIME_FORMATTERS){
                try {
                    return LocalDateTime.parse(txt, fmt);
                } catch (DateTimeParseException ignored) {}
            }

            //falback: solo fecha (sin hora)
            try {
                LocalDate d = LocalDate.parse(txt, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return d.atStartOfDay();
            } catch (DateTimeParseException ignored) {}

            try {
                LocalDate d = LocalDate.parse(txt, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return d.atStartOfDay();
            } catch (DateTimeParseException ignored) {}

            return null;
        } catch (Exception e) {
            log.debug("leerFechaHora error: {}", e.getMessage());
            return null;
        }
    }
    
    // ===== Exportacion de Reporte Ejecutivo (Top15 + Alertas) =====
    /*
     * Genera un Excel con dos hojas:
     * - Top15: skuId, sku, producto, talla, color, unidades, ingresos
     * - Alertas: skuId, sku, stockActual, minStock, coberturaDias, rop, semaforo
     * @return bytes del archivo XLSX
     */
    public byte[] crearReporteEjecutivo(List<TopProductoDTO> top, List<AlertaStockDTO> alertas){
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()){
            CellStyle header = crearHeaderStyle(wb);

            //Hoja 1: Top-15
            Sheet s1 = wb.createSheet("Top15");
            int r = 0;
            Row h1 = s1.createRow(r++);
            String[] cols1 = {"skuId","sku","producto","talla","color","unidades","ingresos"};
            for(int c = 0; c < cols1.length; c++){
                Cell cell = h1.createCell(c);
                cell.setCellValue(cols1[c]);
                cell.setCellStyle(header);
            }
            if (top != null) {
                for (TopProductoDTO t : top){
                    Row row = s1.createRow(r++);
                    int c = 0;
                    row.createCell(c++).setCellValue(nullSafeLong(t.getSkuId()));
                    row.createCell(c++).setCellValue(nvl(t.getSku()));
                    row.createCell(c++).setCellValue(nvl(t.getProducto()));
                    row.createCell(c++).setCellValue(nvl(t.getTalla()));
                    row.createCell(c++).setCellValue(nvl(t.getColor()));
                    row.createCell(c++).setCellValue(t.getUnidades() != null ? t.getUnidades() : 0);
                    row.createCell(c++).setCellValue(t.getIngresos() != null ? t.getIngresos().doubleValue() : 0d);
                }               
            }
            autosize(s1, cols1.length);

            //Hoja 2: Alertas
            Sheet s2 = wb.createSheet("Alertas");
            r = 0;
            Row h2 = s2.createRow(r++);
            String[] cols2 = {"skuId","sku","stockActual","minStock","coberturaDias","rop","semaforo"};
            for (int c = 0; c < cols2.length; c++) {
                Cell cell = h2.createCell(c);
                cell.setCellValue(cols2[c]);
                cell.setCellStyle(header);
            }
            if (alertas != null) {
                for (AlertaStockDTO a : alertas) {
                Row row = s2.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(nullSafeLong(a.getSkuId()));
                row.createCell(c++).setCellValue(nvl(a.getSku()));
                row.createCell(c++).setCellValue(a.getStockActual() != null ? a.getStockActual() : 0);
                row.createCell(c++).setCellValue(a.getMinStock() != null ? a.getMinStock() : 0);
                row.createCell(c++).setCellValue(a.getCoberturaDias() != null ? a.getCoberturaDias() : 0);
                row.createCell(c++).setCellValue(a.getRop() != null ? a.getRop() : 0);
                row.createCell(c++).setCellValue(nvl(a.getSemaforo()));
                }
            }
            autosize(s2, cols2.length);

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando reporte ejecutivo XLSX: " + e.getMessage(), e);
        }
    }

    // ===== Helpers privados =====
    private Integer parseEnteroSeguro(String s){
        if (StringUtils.isBlank(s)) return null;
        try {
            return Integer.valueOf(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseDecimalSeguro(String s) {
        if (StringUtils.isBlank(s)) return null;
        try {
        // quitar separadores de miles comunes
        String norm = s.trim().replace(" ", "").replace(",", "");
        return new BigDecimal(norm);
        } catch (NumberFormatException e) {
        return null;
        }
    }

    private long nullSafeLong(Long v) {
        return v != null ? v : 0L;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
        sheet.autoSizeColumn(i);
        // ancho mínimo razonable
        int current = sheet.getColumnWidth(i);
        int min = 3000; // ≈ 20-25 caracteres
        if (current < min) sheet.setColumnWidth(i, min);
        }
    }

    private CellStyle crearHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
