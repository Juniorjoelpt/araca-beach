package com.aracabeach.config;

import com.aracabeach.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Remove diariamente registros de auditoria mais antigos que a retencao configurada. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditoriaRetencaoScheduler {

    private final AuditoriaService auditoriaService;

    @Value("${araca-beach.auditoria.retencao-dias:365}")
    private int retencaoDias;

    @Scheduled(cron = "0 30 3 * * *")
    public void expurgar() {
        try {
            int removidos = auditoriaService.expirarAntigos(retencaoDias);
            if (removidos > 0) log.info("Auditoria: {} registro(s) com mais de {} dias removido(s).", removidos, retencaoDias);
        } catch (RuntimeException e) {
            log.warn("Falha ao expurgar auditoria: {}", e.getMessage());
        }
    }
}
