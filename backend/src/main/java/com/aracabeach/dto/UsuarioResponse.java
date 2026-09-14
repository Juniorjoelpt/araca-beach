package com.aracabeach.dto;

import com.aracabeach.domain.usuario.Perfil;

/** Nunca inclui a senha/hash - so o que a tela de gestao de usuarios precisa mostrar. */
public record UsuarioResponse(
        Long id,
        String nome,
        String login,
        Perfil perfil,
        boolean ativo
) {
}
