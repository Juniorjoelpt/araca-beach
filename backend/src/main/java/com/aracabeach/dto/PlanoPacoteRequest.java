package com.aracabeach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlanoPacoteRequest(
        @NotBlank String nome,
        String tipo,
        @Min(1) int quantidadeAulas,
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @Min(1) int validadeDias,
        boolean ativo
) {
}
