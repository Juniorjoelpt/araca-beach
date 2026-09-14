package com.aracabeach.dto;

import com.aracabeach.domain.despesa.CategoriaDespesa;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaRequest(
        @NotBlank String descricao,
        @NotNull CategoriaDespesa categoria,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
        @NotNull LocalDate dataVencimento,
        String observacoes
) {
}
