package com.aracabeach.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TurmaRequest(
        @NotBlank String nome,
        // Modalidade esportiva da turma (ex.: "VOLEI", "FUTEVOLEI", "BEACH_TENNIS") -
        // mesmos valores usados em Quadra.tipo/Professor.especialidade. NAO e o
        // TipoAula (PARTICULAR/TURMA) usado em Aula - sao conceitos diferentes que
        // compartilhavam o mesmo nome de campo por engano.
        @NotBlank String tipo,
        @NotNull Long professorId,
        @NotNull Long quadraId,
        // Um ou mais horarios semanais (ex.: seg/qua/sex as 17h, 18h e 20h).
        @NotEmpty @Valid List<TurmaHorarioDto> horarios,
        @Min(1) Integer capacidadeMaxima
) {
}
