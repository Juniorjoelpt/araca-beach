package com.aracabeach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String novaSenha
) {
}
