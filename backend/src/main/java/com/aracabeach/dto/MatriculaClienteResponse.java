package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MatriculaClienteResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        BigDecimal valorMensal,
        int diaVencimento,
        LocalDate dataInicio,
        boolean ativa,
        List<MatriculaClienteHorarioResponse> horarios,
        // Preenchido so na criacao: resumo de quantas ocorrencias foram
        // agendadas e quantas tiveram conflito de horario e foram puladas.
        String resumoGeracao
) {
}
