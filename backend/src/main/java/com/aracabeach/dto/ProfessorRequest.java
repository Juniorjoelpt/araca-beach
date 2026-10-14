package com.aracabeach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProfessorRequest(
        @NotBlank String nome,
        String telefone,
        @NotBlank String especialidade,
        BigDecimal percentualComissao
) {
}
