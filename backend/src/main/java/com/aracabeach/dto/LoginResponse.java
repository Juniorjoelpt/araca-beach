package com.aracabeach.dto;

public record LoginResponse(
        String token,
        String nome,
        String perfil
) {
}
