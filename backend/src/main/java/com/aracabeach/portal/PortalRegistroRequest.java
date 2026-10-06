package com.aracabeach.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortalRegistroRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank(message = "Informe seu telefone") String telefone,
        @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String senha
) {
}
