package com.aracabeach.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MatriculaClienteRequest(
        @NotNull Long clienteId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valorMensal,
        @Min(1) @Max(28) int diaVencimento,
        @NotNull LocalDate dataInicio,
        @NotEmpty List<@Valid MatriculaClienteHorarioRequest> horarios
) {
}
