package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record RegistrarPagamentoMatriculaClienteRequest(
        @NotNull FormaPagamento formaPagamento,
        /** Opcional: valor realmente cobrado (promocao/desconto). Vazio = valor da cobranca. */
        java.math.BigDecimal valor
) {
}
