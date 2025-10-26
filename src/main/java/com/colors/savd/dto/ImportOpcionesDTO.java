package com.colors.savd.dto;

import com.colors.savd.model.enums.AsignacionTemporadaModo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Opciones de importación de ventas desde Excel.
 * - modoTemporada: AUTOMATICA | FIJAR | NINGUNA
 * - temporadaId: requerido si modo = FIJAR (de lo contrario, ignorado)
 * - observacionGeneral: texto opcional que se agregará a la observación de Kardex por fila
 * - soloValidar: si true, no inserta en BD (dry-run); si false, ejecuta alta normal
 */
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class ImportOpcionesDTO {

    @Builder.Default
    private AsignacionTemporadaModo modoTemporada = AsignacionTemporadaModo.AUTOMATICA;

    /* Usado solo si modoTemporada == FIJAR */
    private Long temporadaId;

    /* Texto libre opcional que se concatenara a la observacion de Kardex */
    private String observacionGeneral;

    /* Si true, solo valida y reporta errores; no persiste */
    @Builder.Default
    private boolean soloValidar = false;
}
