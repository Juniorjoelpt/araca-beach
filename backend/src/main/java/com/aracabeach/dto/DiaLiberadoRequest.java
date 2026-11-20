package com.aracabeach.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DiaLiberadoRequest(@NotNull LocalDate data, String motivo) {
}
