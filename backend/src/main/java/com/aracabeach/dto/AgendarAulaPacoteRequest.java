package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendarAulaPacoteRequest(
        @NotNull Long professorId,
        @NotNull Long quadraId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim,
        Long reposicaoDeAulaId,
        String observacoes
) {
}
