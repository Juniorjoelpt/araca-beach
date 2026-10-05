package com.aracabeach.config;

import com.aracabeach.service.MatriculaClienteService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gera a cobranca mensal (01:25, depois de MatriculaScheduler) e estende o
 * horizonte de ocorrencias (01:30) das matriculas de cliente com mensalidade
 * combinada, mesmo racional dos outros schedulers do sistema.
 */
@Component
@RequiredArgsConstructor
public class MatriculaClienteScheduler {

    private final MatriculaClienteService matriculaClienteService;

    @PostConstruct
    public void aoIniciar() {
        matriculaClienteService.gerarCobrancasDoMes();
        matriculaClienteService.estenderHorizonte();
    }

    @Scheduled(cron = "0 25 1 * * *")
    public void gerarCobrancasDoMes() {
        matriculaClienteService.gerarCobrancasDoMes();
    }

    @Scheduled(cron = "0 30 1 * * *")
    public void estenderHorizonte() {
        matriculaClienteService.estenderHorizonte();
    }
}
