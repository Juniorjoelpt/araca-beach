package com.aracabeach.dto;

import java.util.List;

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
        boolean ativa,
        List<Horario> horarios
) {
    /** diaSemana (MONDAY...) e horas no formato HH:mm. */
    public record Horario(String diaSemana, String horaInicio, String horaFim) {
    }
}
