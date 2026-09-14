package com.aracabeach.dto;

import com.aracabeach.domain.despesa.CategoriaDespesa;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DespesaRecorrenteRequest(
        @NotBlank String descricao,
        @NotNull CategoriaDespesa categoria,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
        @Min(1) @Max(28) int diaVencimento
) {
}
