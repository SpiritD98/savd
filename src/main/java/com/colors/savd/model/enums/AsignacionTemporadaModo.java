package com.colors.savd.model.enums;

/**
 * Define cómo se asignará la temporada a las ventas importadas.
 * - AUTOMATICA: se busca una temporada ACTIVA cuyo rango (inicio/fin) contenga la fecha de la venta.
 * - FIJAR: se usa una temporada específica (requiere temporadaId en las opciones de importación).
 * - NINGUNA: no se asigna temporada (queda NULL).
 */
public enum AsignacionTemporadaModo {
    AUTOMATICA,
    FIJAR,
    NINGUNA
}
