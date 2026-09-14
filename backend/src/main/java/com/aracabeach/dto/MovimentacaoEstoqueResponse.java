package com.aracabeach.dto;

import java.time.LocalDateTime;

public record MovimentacaoEstoqueResponse(
        Long id,
        String produtoNome,
        String tipo,
        int quantidade,
        String motivo,
        LocalDateTime criadoEm
) {
}
