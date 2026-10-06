package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record RegraPrecoResponse(
        Long id,
        String nome,
        Long quadraId,
        String quadraNome,
        List<String> diasSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        BigDecimal valorHora,
        boolean ativa
) {
}
