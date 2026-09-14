package com.aracabeach.dto;

import java.time.LocalDateTime;

public record AulaResumoResponse(
        Long aulaId,
        String professorNome,
        String quadraNome,
        LocalDateTime inicio,
        LocalDateTime fim
) {
}
