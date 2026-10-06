package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComissaoLancamentoResponse(
        Long id,
        Long professorId,
        String professorNome,
        String origem,
        String descricao,
        LocalDate competencia,
        BigDecimal base,
        BigDecimal percentual,
        BigDecimal valor,
        String status,
        LocalDate dataPagamento
) {
}
