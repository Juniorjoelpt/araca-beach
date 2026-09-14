package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComissaoProfessorResponse(
        Long professorId,
        String professorNome,
        LocalDate inicio,
        LocalDate fim,
        int quantidadeAulas,
        BigDecimal valorTotalAulas,
        BigDecimal percentualComissao,
        BigDecimal valorComissao
) {
}
