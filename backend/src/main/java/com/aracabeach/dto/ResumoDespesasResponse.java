package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoDespesasResponse(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal totalPago,
        BigDecimal totalPendente,
        BigDecimal totalGeral
) {
}
