package com.aracabeach.domain.pacote;

public enum StatusAulaPacote {
    AGENDADA,
    REALIZADA,
    FALTA_AVISADA,
    FALTA_SEM_AVISO,
    CANCELADA;

    /** Consome credito do pacote (FALTA_AVISADA e CANCELADA devolvem o credito = reposicao). */
    public boolean consomeCredito() {
        return this == AGENDADA || this == REALIZADA || this == FALTA_SEM_AVISO;
    }
}
