package com.aracabeach.dto;

import com.aracabeach.domain.quadra.TipoQuadra;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProfessorRequest(
        @NotBlank String nome,
        String telefone,
        @NotNull TipoQuadra especialidade,
        BigDecimal percentualComissao
) {
}
