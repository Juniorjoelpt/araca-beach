package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

public record ComandaRequest(
        @NotNull Long clienteId,
        Long reservaId
) {
}
