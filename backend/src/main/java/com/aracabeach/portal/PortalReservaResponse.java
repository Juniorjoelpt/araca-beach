package com.aracabeach.portal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * taxaSeCancelarAgora / cancelamentoGratisAte so vem preenchidos para reservas
 * confirmadas e futuras. taxaCancelamento = multa efetivamente aplicada.
 */
public record PortalReservaResponse(
        Long id,
        String quadraNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        String status,
        BigDecimal valorTotal,
        String statusPagamento,
        BigDecimal taxaCancelamento,
        BigDecimal taxaSeCancelarAgora,
        LocalDateTime cancelamentoGratisAte,
        String mensagemCancelamento
) {
}
