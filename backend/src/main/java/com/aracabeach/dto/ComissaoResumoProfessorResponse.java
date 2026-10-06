package com.aracabeach.dto;

import java.math.BigDecimal;

public record ComissaoResumoProfessorResponse(
        Long professorId,
        String professorNome,
        BigDecimal percentual,
        int quantidadePendentes,
        BigDecimal valorPendente,
        BigDecimal valorPago
) {
}
