package com.aracabeach.domain.dayuse;

public enum StatusDayUse {
    /** Reserva feita online, aguardando pagamento (ocupa vaga, mas nao entra no caixa nem libera a entrada). */
    PENDENTE,
    ATIVA,
    CANCELADA
}
