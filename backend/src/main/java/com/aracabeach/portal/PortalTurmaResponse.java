package com.aracabeach.portal;

import java.time.LocalTime;
import java.util.List;

public record PortalTurmaResponse(
        Long matriculaId,
        String turmaNome,
        String tipo,
        String professorNome,
        String quadraNome,
        String diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        List<Horario> horarios
) {
    /** Todos os horarios da turma (diaSemana = MONDAY...). */
    public record Horario(String diaSemana, LocalTime horaInicio, LocalTime horaFim) {
    }
}
