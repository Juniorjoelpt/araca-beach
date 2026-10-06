package com.aracabeach.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record RegraPrecoRequest(
        @NotBlank String nome,
        Long quadraId,
        List<String> diasSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull @DecimalMin("0.01") BigDecimal valorHora,
        boolean ativa
) {
}
