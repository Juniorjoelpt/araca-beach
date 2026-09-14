package com.aracabeach.portal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PortalReservaResponse(
        Long id,
        String quadraNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        String status,
        BigDecimal valorTotal,
        String statusPagamento
) {
}
