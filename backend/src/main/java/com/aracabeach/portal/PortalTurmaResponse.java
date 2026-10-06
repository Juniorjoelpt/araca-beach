package com.aracabeach.portal;

import java.time.LocalTime;

public record PortalTurmaResponse(
        Long matriculaId,
        String turmaNome,
        String tipo,
        String professorNome,
        String quadraNome,
        String diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim
) {
}
