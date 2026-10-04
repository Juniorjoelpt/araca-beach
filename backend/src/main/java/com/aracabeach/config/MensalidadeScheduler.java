package com.aracabeach.config;

import com.aracabeach.service.MensalidadeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Roda uma vez por dia via cron, e tambem uma vez ao iniciar o sistema
 * (@PostConstruct) - mesmo racional do DespesaRecorrenteScheduler: cobre o
 * caso de o backend ficar desligado durante a virada do mes.
 */
@Component
@RequiredArgsConstructor
public class MensalidadeScheduler {

    private final MensalidadeService mensalidadeService;

    @PostConstruct
    public void aoIniciar() {
        mensalidadeService.gerarCobrancasDoMes();
    }

    // Roda todo dia a 01:10 (10 minutos apos a geracao das despesas recorrentes)
    @Scheduled(cron = "0 10 1 * * *")
    public void gerarCobrancasDoMes() {
        mensalidadeService.gerarCobrancasDoMes();
    }
}
