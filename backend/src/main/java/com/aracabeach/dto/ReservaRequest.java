package com.aracabeach.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservaRequest(
        @NotNull Long quadraId,
        @NotNull Long clienteId,
        @NotNull @Future LocalDateTime inicio,
        @NotNull @Future LocalDateTime fim,
        String observacoes
) {
}
