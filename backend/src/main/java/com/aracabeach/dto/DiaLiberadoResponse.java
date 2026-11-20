package com.aracabeach.dto;

import java.time.LocalDate;

public record DiaLiberadoResponse(Long id, LocalDate data, String motivo) {
}
