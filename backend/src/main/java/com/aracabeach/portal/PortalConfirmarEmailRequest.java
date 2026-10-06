package com.aracabeach.portal;

import jakarta.validation.constraints.NotBlank;

public record PortalConfirmarEmailRequest(@NotBlank String token) {
}
