package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoMatriculaResponse(
        Long id,
        Long matriculaId,
        String clienteNome,
        String turmaNome,
        String referenciaMes,
        BigDecimal valor,
        LocalDate vencimento,
        boolean pago,
        LocalDate dataPagamento,
        FormaPagamento formaPagamento
) {
}
