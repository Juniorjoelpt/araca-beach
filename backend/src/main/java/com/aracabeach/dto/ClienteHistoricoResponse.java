package com.aracabeach.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClienteHistoricoResponse(
        Long clienteId,
        String clienteNome,
        List<ReservaHistoricoItem> reservas,
        List<ComandaHistoricoItem> comandas,
        BigDecimal totalGastoReservas,
        BigDecimal totalGastoLoja
) {
    public record ReservaHistoricoItem(
            Long reservaId,
            String quadraNome,
            java.time.LocalDateTime inicio,
            java.time.LocalDateTime fim,
            String status,
            BigDecimal valorTotal
    ) {
    }

    public record ComandaHistoricoItem(
            Long comandaId,
            java.time.LocalDateTime criadoEm,
            boolean fechada,
            BigDecimal total
    ) {
    }
}
