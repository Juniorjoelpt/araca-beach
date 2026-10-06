package com.aracabeach.dto;

import java.time.LocalDateTime;

public record BloqueioQuadraResponse(
        Long id,
        Long quadraId,
        String quadraNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        String motivo,
        int reservasAfetadas
) {
}
