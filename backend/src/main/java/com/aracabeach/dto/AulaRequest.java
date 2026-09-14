package com.aracabeach.dto;

import com.aracabeach.domain.aula.TipoAula;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AulaRequest(
        @NotNull Long professorId,
        @NotNull Long quadraId,
        @NotNull TipoAula tipo,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fim,
        List<String> alunos,
        BigDecimal valor
) {
}
