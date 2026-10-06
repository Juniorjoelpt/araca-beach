package com.aracabeach.portal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * motivoIndisponivel: null (livre), OCUPADO (ha reserva - da para entrar na
 * lista de espera), BLOQUEADO (manutencao/evento) ou PASSADO.
 * preco = valor do slot ja considerando pico/fora de pico (sem desconto de mensalista).
 */
public record PortalSlotResponse(
        LocalDateTime inicio,
        LocalDateTime fim,
        boolean disponivel,
        BigDecimal preco,
        String motivoIndisponivel
) {
}
