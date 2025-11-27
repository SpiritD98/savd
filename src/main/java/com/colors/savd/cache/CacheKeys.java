package com.colors.savd.cache;

import java.time.LocalDateTime;

public final class CacheKeys {
  private CacheKeys() {}

  public static LocalDateTime dayStart(LocalDateTime dt) {
    return (dt == null) ? null : dt.toLocalDate().atStartOfDay();
  }
  public static LocalDateTime dayEnd(LocalDateTime dt) {
    return (dt == null) ? null : dt.toLocalDate().atTime(23,59,59);
  }

  public static String top15(LocalDateTime desde, LocalDateTime hasta, Long canalId) {
    return dayStart(desde) + "|" + dayEnd(hasta) + "|" + String.valueOf(canalId);
  }

  public static String kpiKey(LocalDateTime d, LocalDateTime h,
                              Long canal, Long temp, Long cat, Long talla, Long color) {
    return dayStart(d) + "|" + dayEnd(h) + "|" +
           String.valueOf(canal) + "|" + String.valueOf(temp) + "|" +
           String.valueOf(cat) + "|" + String.valueOf(talla) + "|" + String.valueOf(color);
  }

  public static String alertas(LocalDateTime corte) {
    return (corte == null ? "now" : String.valueOf(dayEnd(corte)));
  }
}
