package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Simulacao do que acontece se a reserva for cancelada agora. */
public record PoliticaCancelamentoResponse(
        boolean gratis,
        BigDecimal taxa,
        int horasCancelamentoGratis,
        BigDecimal percentualMulta,
        LocalDateTime gratisAte,
        String mensagem
) {
}
