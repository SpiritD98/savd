package com.colors.savd.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cm = new CaffeineCacheManager(
        "kpiCategoriaMensual","kpiProductoMensual","kpiSkuMensual",
        "kpiCategoriaTotal","kpiProductoTotal","kpiSkuTotal",
        "top15","alertasStock"
    );
    cm.setCaffeine(
        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
          .maximumSize(500)                 // ajusta a tu memoria
          .expireAfterWrite(Duration.ofMinutes(10)) // caché caliente 10 min
    );
    return cm;
  }
}
