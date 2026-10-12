package com.aracabeach.config;

import com.aracabeach.service.TurmaService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mantem as turmas ocupando a quadra nas proximas semanas (roda ao iniciar e todo dia). */
@Component
@RequiredArgsConstructor
public class TurmaBloqueioScheduler {

    private final TurmaService turmaService;

    @PostConstruct
    public void aoIniciar() {
        turmaService.sincronizarTodas();
    }

    @Scheduled(cron = "0 30 1 * * *")
    public void diario() {
        turmaService.sincronizarTodas();
    }
}
