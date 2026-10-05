package com.aracabeach.config;

import com.aracabeach.service.MatriculaService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mesmo racional de MensalidadeScheduler: gera a cobranca do mes para toda
 * matricula ativa, uma vez por dia e tambem ao iniciar o sistema (cobre o
 * caso de o backend ficar desligado durante a virada do mes).
 */
@Component
@RequiredArgsConstructor
public class MatriculaScheduler {

    private final MatriculaService matriculaService;

    @PostConstruct
    public void aoIniciar() {
        matriculaService.gerarCobrancasDoMes();
    }

    // Roda todo dia a 01:15 (entre mensalidades de quadra e a extensao de recorrencias)
    @Scheduled(cron = "0 15 1 * * *")
    public void gerarCobrancasDoMes() {
        matriculaService.gerarCobrancasDoMes();
    }
}
