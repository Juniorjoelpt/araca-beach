package com.aracabeach.portal;

import jakarta.validation.constraints.NotBlank;

public record PortalPerfilRequest(
        @NotBlank String nome,
        @NotBlank String telefone
) {
}
