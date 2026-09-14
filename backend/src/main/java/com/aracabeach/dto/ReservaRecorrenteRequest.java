package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRecorrenteRequest(
        @NotNull Long quadraId,
        @NotNull Long clienteId,
        @NotNull DayOfWeek diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull LocalDate vigenciaInicio,
        LocalDate vigenciaFim
) {
}
