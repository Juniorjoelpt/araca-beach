package com.aracabeach.dto;

import com.aracabeach.domain.pacote.StatusAulaPacote;
import jakarta.validation.constraints.NotNull;

/** Chamada: REALIZADA, FALTA_AVISADA (devolve credito), FALTA_SEM_AVISO (consome) ou CANCELADA (devolve). */
public record PresencaAulaRequest(@NotNull StatusAulaPacote status) {
}
