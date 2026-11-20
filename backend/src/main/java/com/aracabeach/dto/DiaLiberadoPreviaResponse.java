package com.aracabeach.dto;

import java.time.LocalDate;
import java.util.List;

/** O que acontece (ou aconteceu) ao liberar um dia. */
public record DiaLiberadoPreviaResponse(
        LocalDate data,
        boolean jaLiberado,
        List<Item> reservasCanceladas,
        List<Item> turmasLiberadas,
        List<Item> aulasCanceladas,
        List<Item> avisos
) {
    public record Item(Long id, String hora, String quadra, String descricao) {
    }
}
