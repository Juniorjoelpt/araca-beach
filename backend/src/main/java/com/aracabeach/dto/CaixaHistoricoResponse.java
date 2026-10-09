package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Extrato de recebimentos do caixa em um periodo, com totais por forma, origem e operador. */
public record CaixaHistoricoResponse(
        LocalDate inicio,
        LocalDate fim,
        String operadorFiltro,
        BigDecimal total,
        long quantidade,
        Map<String, BigDecimal> porForma,
        Map<String, BigDecimal> porOrigem,
        Map<String, BigDecimal> porOperador,
        List<Lancamento> lancamentos,
        BigDecimal totalCortesias,
        List<Cortesia> cortesias
) {
    /** Venda cortesia (restaurante): nao entra no total do caixa, mas fica registrada. */
    public record Cortesia(
            Long id,
            LocalDateTime dataHora,
            String descricao,
            String motivo,
            BigDecimal valorReferencia,
            String operador,
            String operadorNome
    ) {}

    public record Lancamento(
            Long id,
            LocalDateTime dataHora,
            String origem,
            String descricao,
            String forma,
            BigDecimal valor,
            String operador,
            String operadorNome
    ) {}
}
