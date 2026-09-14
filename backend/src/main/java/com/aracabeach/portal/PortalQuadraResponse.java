package com.aracabeach.portal;

import java.math.BigDecimal;

/** Visao publica de uma quadra - so o que o cliente precisa ver para escolher onde reservar. */
public record PortalQuadraResponse(
        Long id,
        String nome,
        String tipo,
        BigDecimal valorHora,
        Integer capacidade
) {
}
