package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoRequest(
        @NotNull Long reservaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
        @NotNull FormaPagamento formaPagamento,
        boolean ehSinal
) {
}
