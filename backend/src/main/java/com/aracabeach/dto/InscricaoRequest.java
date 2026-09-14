package com.aracabeach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InscricaoRequest(
        @NotNull Long torneioId,
        @NotBlank String participante,
        String parceiro,
        @NotBlank String categoria
) {
}
