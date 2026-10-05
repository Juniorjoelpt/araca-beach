package com.aracabeach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TurmaRequest(
        @NotBlank String nome,
        // Modalidade esportiva da turma (ex.: "VOLEI", "FUTEVOLEI", "BEACH_TENNIS") -
        // mesmos valores usados em Quadra.tipo/Professor.especialidade. NAO e o
        // TipoAula (PARTICULAR/TURMA) usado em Aula - sao conceitos diferentes que
        // compartilhavam o mesmo nome de campo por engano.
        @NotBlank String tipo,
        @NotNull Long professorId,
        @NotNull Long quadraId,
        @NotNull DayOfWeek diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @Min(1) Integer capacidadeMaxima
) {
}
