package com.aracabeach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MovimentacaoEstoqueRequest(
        @NotNull Long produtoId,
        @NotNull @Min(1) Integer quantidade,
        String motivo
) {
}
