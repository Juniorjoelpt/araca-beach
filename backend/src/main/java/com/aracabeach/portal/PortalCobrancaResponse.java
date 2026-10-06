package com.aracabeach.portal;

import java.math.BigDecimal;
import java.time.LocalDate;

/** tipo: MENSALIDADE | MATRICULA | TURMA | PACOTE | RESERVA | MULTA */
public record PortalCobrancaResponse(
        String tipo,
        String descricao,
        BigDecimal valor,
        LocalDate vencimento,
        boolean vencida
) {
}
