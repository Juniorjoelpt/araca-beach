package com.aracabeach.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrarResultadoRequest(
        @NotBlank String vencedor,
        String placar
) {
}
