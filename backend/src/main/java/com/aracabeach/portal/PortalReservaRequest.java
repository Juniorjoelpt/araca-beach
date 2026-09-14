package com.aracabeach.portal;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PortalReservaRequest(
        @NotNull Long quadraId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim
) {
}
