package com.aracabeach.dto;

public record TurmaResponse(
        Long id,
        String nome,
        String tipo,
        Long professorId,
        String professorNome,
        Long quadraId,
        String quadraNome,
        String diaSemana,
        String horaInicio,
        String horaFim,
        Integer capacidadeMaxima,
        int matriculasAtivas,
        boolean ativa
) {
}
