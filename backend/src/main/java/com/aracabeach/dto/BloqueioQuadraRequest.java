package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** quadraId nulo = bloqueia todas as quadras. */
public record BloqueioQuadraRequest(
        Long quadraId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim,
        String motivo
) {
}
