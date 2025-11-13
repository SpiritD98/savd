package com.colors.savd.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ParametroReposicionDTO {
    private Long id;
    
    @NotNull
    private Long skuId;

    @NotNull @Min(0)
    private Integer minStock;

    @NotNull @Min(0)
    private Integer leadTimeDias;

    @NotNull @Min(0)
    private Integer stockSeguridad;
}
