package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ListaEsperaRequest(
        @NotNull Long quadraId,
        Long clienteId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim
) {
}
