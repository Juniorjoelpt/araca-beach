package com.aracabeach.dto;

import java.time.LocalDateTime;

public record ListaEsperaResponse(
        Long id,
        Long quadraId,
        String quadraNome,
        Long clienteId,
        String clienteNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        String status,
        LocalDateTime criadoEm
) {
}
