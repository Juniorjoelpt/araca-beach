package com.aracabeach.dto;

import jakarta.validation.constraints.NotBlank;

public record GerarChaveamentoRequest(
        @NotBlank String categoria
) {
}
