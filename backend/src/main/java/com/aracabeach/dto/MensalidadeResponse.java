package com.aracabeach.dto;

import java.math.BigDecimal;

/**
 * Representa a mensalidade junto com dados da recorrencia associada, para
 * exibir direto na listagem sem o frontend precisar cruzar as duas tabelas.
 */
public record MensalidadeResponse(
        Long id,
        Long reservaRecorrenteId,
        String clienteNome,
        String quadraNome,
        String diaSemana,
        String horaInicio,
        String horaFim,
        BigDecimal valorMensal,
        int diaVencimento,
        boolean ativa
) {
}
