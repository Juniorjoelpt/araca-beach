package com.aracabeach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AjusteEstoqueRequest(
        @NotNull Long produtoId,
        @NotNull @Min(0) Integer novoEstoque,
        String motivo
) {
}
