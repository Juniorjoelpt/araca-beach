package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linha da secao "Matriculas" do Financeiro: situacao do mes de cada aluno matriculado. */
public record MatriculaFinanceiroResponse(
        Long matriculaId,
        Long pagamentoId,
        String clienteNome,
        String turmaNome,
        String referenciaMes,
        BigDecimal valor,
        LocalDate vencimento,
        boolean pago,
        boolean vencida,
        LocalDate dataPagamento,
        FormaPagamento formaPagamento
) {
}
