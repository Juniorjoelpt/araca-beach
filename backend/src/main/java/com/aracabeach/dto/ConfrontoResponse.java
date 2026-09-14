package com.aracabeach.dto;

public record ConfrontoResponse(
        Long id,
        int rodada,
        int posicao,
        String participanteA,
        String participanteB,
        String vencedor,
        String placar
) {
}
