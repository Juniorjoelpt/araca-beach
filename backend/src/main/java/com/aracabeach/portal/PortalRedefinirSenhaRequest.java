package com.aracabeach.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortalRedefinirSenhaRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String novaSenha
) {
}
