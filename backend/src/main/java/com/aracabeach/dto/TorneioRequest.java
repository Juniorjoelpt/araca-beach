package com.aracabeach.dto;

import com.aracabeach.domain.quadra.TipoQuadra;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record TorneioRequest(
        @NotBlank String nome,
        @NotNull TipoQuadra modalidade,
        @NotNull LocalDate dataInicio,
        LocalDate dataFim,
        List<String> categorias
) {
}
