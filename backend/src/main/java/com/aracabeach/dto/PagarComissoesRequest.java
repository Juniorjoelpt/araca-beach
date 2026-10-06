package com.aracabeach.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** lancarDespesa = registra o pagamento como Despesa (categoria COMISSAO) no financeiro. */
public record PagarComissoesRequest(
        @NotNull Long professorId,
        @NotEmpty List<Long> ids,
        boolean lancarDespesa
) {
}
