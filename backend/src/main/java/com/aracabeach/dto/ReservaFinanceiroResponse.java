package com.aracabeach.dto;

import com.aracabeach.domain.reserva.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Visao da reserva com o total pago, usada na tela de Financeiro
 * para mostrar rapidamente o que esta pendente/pago/parcial.
 */
public record ReservaFinanceiroResponse(
        Long reservaId,
        String quadraNome,
        String clienteNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        StatusReserva statusReserva,
        BigDecimal valorTotal,
        BigDecimal valorPago,
        String statusPagamento
) {
}
