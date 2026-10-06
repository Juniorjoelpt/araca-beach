package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PacoteClienteResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        Long planoId,
        String planoNome,
        String tipo,
        int aulasTotal,
        int aulasConsumidas,
        int saldo,
        BigDecimal valor,
        LocalDate dataCompra,
        LocalDate validade,
        boolean pago,
        LocalDate dataPagamento,
        String formaPagamento,
        String situacao // ATIVO | ESGOTADO | VENCIDO | CANCELADO
) {
}
