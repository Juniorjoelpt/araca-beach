package com.aracabeach.dto;

import java.time.LocalDateTime;

public record AulaPacoteResponse(
        Long id,
        Long pacoteId,
        Long clienteId,
        String clienteNome,
        String planoNome,
        Long professorId,
        String professorNome,
        Long quadraId,
        String quadraNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        String status,
        Long reposicaoDeAulaId,
        String observacoes
) {
}
