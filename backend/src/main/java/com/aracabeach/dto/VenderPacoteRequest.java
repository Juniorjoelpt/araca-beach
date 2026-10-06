package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.validation.constraints.NotNull;

/** Se formaPagamento vier preenchida, o pacote ja nasce pago (entra no caixa). */
public record VenderPacoteRequest(
        @NotNull Long clienteId,
        @NotNull Long planoId,
        FormaPagamento formaPagamento
) {
}
