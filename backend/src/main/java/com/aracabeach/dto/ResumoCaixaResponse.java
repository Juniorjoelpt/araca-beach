package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ResumoCaixaResponse(
        LocalDate data,
        BigDecimal totalRecebido,
        Map<String, BigDecimal> porFormaPagamento,
        long quantidadePagamentos
) {
}
