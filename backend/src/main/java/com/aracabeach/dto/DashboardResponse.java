package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate data,
        int totalQuadras,
        int reservasAtivas,
        int reservasCanceladas,
        BigDecimal faturamentoReservas,
        BigDecimal faturamentoLoja,
        BigDecimal faturamentoTotal,
        List<AulaResumoResponse> proximasAulas,
        List<ReservaFinanceiroResponse> reservasPendentes
) {
}
