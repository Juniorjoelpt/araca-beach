package com.aracabeach.dto;

import com.aracabeach.domain.usuario.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank String nome,
        @NotBlank String login,
        @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres") String senha,
        @NotNull Perfil perfil
) {
}
