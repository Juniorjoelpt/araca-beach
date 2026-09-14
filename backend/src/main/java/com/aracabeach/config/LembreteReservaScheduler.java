package com.aracabeach.config;

import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.repository.ReservaRepository;
import com.aracabeach.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A cada 15 minutos, verifica reservas confirmadas cujo horario de inicio
 * cai dentro da janela de lembrete (ex.: entre 1h45 e 2h a partir de agora,
 * para um lembrete de "2 horas antes") e ainda nao foram notificadas.
 * O campo lembreteEnviado evita reenvio quando o job roda de novo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LembreteReservaScheduler {

    private final ReservaRepository reservaRepository;
    private final NotificacaoService notificacaoService;

    @Value("${araca-beach.notificacoes.email.horas-antes-lembrete:2}")
    private long horasAntesLembrete;

    private static final long JANELA_MINUTOS = 15;

    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
    public void enviarLembretes() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioJanela = agora.plusHours(horasAntesLembrete);
        LocalDateTime fimJanela = inicioJanela.plusMinutes(JANELA_MINUTOS);

        List<Reserva> reservas = reservaRepository.findByStatusAndLembreteEnviadoFalseAndInicioBetween(
                StatusReserva.CONFIRMADA, inicioJanela, fimJanela);

        if (reservas.isEmpty()) {
            return;
        }

        log.info("Enviando lembrete para {} reserva(s) que começam entre {}h e {}h a partir de agora.",
                reservas.size(), horasAntesLembrete, horasAntesLembrete);

        for (Reserva reserva : reservas) {
            notificacaoService.enviarLembrete(reserva);
            reserva.setLembreteEnviado(true);
        }
        reservaRepository.saveAll(reservas);
    }
}
