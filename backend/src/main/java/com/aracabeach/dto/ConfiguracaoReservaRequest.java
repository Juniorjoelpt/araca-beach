package com.aracabeach.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfiguracaoReservaRequest(
        @Min(0) int horasCancelamentoGratis,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentualMulta,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentualMultaNoShow,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal descontoMensalistaPercentual,
        boolean listaEsperaAtiva,
        @Min(0) int horasAvisoFaltaAula
) {
}
