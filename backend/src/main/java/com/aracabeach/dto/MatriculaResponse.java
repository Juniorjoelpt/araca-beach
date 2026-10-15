package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MatriculaResponse(
        Long id,
        Long turmaId,
        String turmaNome,
        Long clienteId,
        String clienteNome,
        LocalDate dataMatricula,
        BigDecimal valorMensal,
        int diaVencimento,
        boolean ativa,
        FormaPagamento formaPagamento
) {
}
