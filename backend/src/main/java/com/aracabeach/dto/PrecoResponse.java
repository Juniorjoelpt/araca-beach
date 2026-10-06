package com.aracabeach.dto;

import java.math.BigDecimal;

public record PrecoResponse(
        BigDecimal valorBruto,
        BigDecimal desconto,
        BigDecimal valorTotal,
        boolean mensalista,
        int minutos
) {
}
