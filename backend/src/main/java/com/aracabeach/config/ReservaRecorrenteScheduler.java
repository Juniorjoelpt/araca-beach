package com.aracabeach.config;

import com.aracabeach.service.ReservaRecorrenteService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Antes deste scheduler, uma recorrencia sem data final (vigenciaFim nulo)
 * parava de gerar novas reservas apos as 12 semanas iniciais criadas no
 * momento do cadastro (HORIZONTE_PADRAO_SEMANAS em ReservaRecorrenteService),
 * e ninguem percebia ate a agenda "acabar do nada" semanas depois. Roda uma
 * vez por dia e tambem ao iniciar o sistema, mesmo racional dos outros
 * schedulers (MensalidadeScheduler, DespesaRecorrenteScheduler).
 */
@Component
@RequiredArgsConstructor
public class ReservaRecorrenteScheduler {

    private final ReservaRecorrenteService reservaRecorrenteService;

    @PostConstruct
    public void aoIniciar() {
        reservaRecorrenteService.estenderHorizonte();
    }

    // Roda todo dia a 01:20 (apos despesas recorrentes e mensalidades)
    @Scheduled(cron = "0 20 1 * * *")
    public void estenderHorizonte() {
        reservaRecorrenteService.estenderHorizonte();
    }
}
