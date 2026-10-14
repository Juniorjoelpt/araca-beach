package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Um horario semanal da turma (dia + inicio + fim). */
public record TurmaHorarioDto(
        @NotNull DayOfWeek diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim
) {
}
