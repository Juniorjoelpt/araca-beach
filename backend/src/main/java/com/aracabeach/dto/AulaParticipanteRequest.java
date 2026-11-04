package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Aluno de aula avulsa. valor nulo = valor padrao por aluno da aula. */
public record AulaParticipanteRequest(
        @NotBlank String nome,
        BigDecimal valor,
        Boolean pago,
        FormaPagamento formaPagamento
) {
}
