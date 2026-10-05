package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoMatriculaClienteResponse(
        Long id,
        Long matriculaClienteId,
        String clienteNome,
        String referenciaMes,
        BigDecimal valor,
        LocalDate vencimento,
        boolean pago,
        LocalDate dataPagamento,
        FormaPagamento formaPagamento
) {
}
