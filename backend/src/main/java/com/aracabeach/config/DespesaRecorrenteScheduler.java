package com.aracabeach.config;

import com.aracabeach.service.DespesaRecorrenteService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Roda uma vez por dia via cron, e tambem uma vez ao iniciar o sistema
 * (@PostConstruct) - isso cobre o caso comum em desenvolvimento de o
 * backend ficar desligado durante a virada do mes e so ser religado
 * depois; sem o PostConstruct, a despesa do mes ficaria pendente ate
 * o proximo horario do cron.
 */
@Component
@RequiredArgsConstructor
public class DespesaRecorrenteScheduler {

    private final DespesaRecorrenteService despesaRecorrenteService;

    @PostConstruct
    public void aoIniciar() {
        despesaRecorrenteService.gerarDespesasDoMes();
    }

    // Roda todo dia a 01:00
    @Scheduled(cron = "0 0 1 * * *")
    public void gerarDespesasDoMes() {
        despesaRecorrenteService.gerarDespesasDoMes();
    }
}
