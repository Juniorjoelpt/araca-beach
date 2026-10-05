package com.aracabeach.dto;

public record MatriculaClienteHorarioResponse(
        Long id,
        Long quadraId,
        String quadraNome,
        String diaSemana,
        String horaInicio,
        String horaFim,
        boolean ativo
) {
}
