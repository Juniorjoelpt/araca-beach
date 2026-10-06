package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa, de forma unificada, uma cobranca pendente de Mensalidade (por
 * ReservaRecorrente), de MatriculaCliente (mensalidade combinada de varios
 * horarios) ou de Matricula de Turma (aula em grupo) - as tres formas de
 * cobranca recorrente do sistema. Usado na pagina Financeiro para dar
 * visibilidade a essas cobrancas, que nao aparecem na "visao do dia" por
 * reserva (ver PagamentoService.visaoFinanceiraDoDia).
 */
public record CobrancaPendenteResponse(
        Long pagamentoId,
        String tipo, // MENSALIDADE | MATRICULA_CLIENTE | MATRICULA_TURMA | PACOTE
        String clienteNome,
        String descricao,
        String referenciaMes,
        BigDecimal valor,
        LocalDate vencimento,
        boolean vencida
) {
}
