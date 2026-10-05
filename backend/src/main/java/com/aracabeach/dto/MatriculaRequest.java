package com.aracabeach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MatriculaRequest(
        @NotNull Long turmaId,
        @NotNull Long clienteId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valorMensal,
        @Min(1) @Max(28) int diaVencimento
) {
}
