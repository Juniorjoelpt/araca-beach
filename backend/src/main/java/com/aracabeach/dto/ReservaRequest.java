package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Nao usamos @Future aqui de proposito: a validacao de "nao pode ser no
 * passado" e feita em ReservaService (comparando a DATA com hoje, nao o
 * instante exato), para permitir registrar uma reserva de hoje mesmo que
 * o horario selecionado ja tenha passado (ex.: cliente chegou sem reserva
 * previa e a recepcao registra na hora). Um 400 com mensagem clara e
 * sempre melhor do que uma falha de @Valid sem feedback para o usuario.
 */
public record ReservaRequest(
        @NotNull Long quadraId,
        @NotNull Long clienteId,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim,
        String observacoes
) {
}
