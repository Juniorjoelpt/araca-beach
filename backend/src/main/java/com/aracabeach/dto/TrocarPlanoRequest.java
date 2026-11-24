package com.aracabeach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Troca o plano (turma) de uma matricula, com o novo valor mensal. */
public record TrocarPlanoRequest(
        @NotNull Long turmaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valorMensal
) {
}
