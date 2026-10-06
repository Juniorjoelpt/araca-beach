package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record PagarPacoteRequest(@NotNull FormaPagamento formaPagamento) {
}
