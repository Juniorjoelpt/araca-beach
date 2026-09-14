package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EstatisticaDiaResponse(
        LocalDate data,
        BigDecimal faturamentoReservas,
        BigDecimal faturamentoLoja,
        BigDecimal faturamentoTotal,
        int quantidadeReservas
) {
}
