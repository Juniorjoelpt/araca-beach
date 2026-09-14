package com.aracabeach.portal;

import jakarta.validation.constraints.NotBlank;

public record PortalLoginRequest(
        @NotBlank String email,
        @NotBlank String senha
) {
}
