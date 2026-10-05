package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.ReservaRecorrente;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.ReservaRecorrenteRequest;
import com.aracabeach.dto.ReservaRecorrenteResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRecorrenteRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Cria a regra de recorrencia e ja gera as reservas individuais dentro
 * do periodo de vigencia. Como nao ha um job agendado gerando semanas
 * futuras indefinidamente, o horizonte de geracao e limitado (ver
 * HORIZONTE_PADRAO_SEMANAS) para nao criar reservas anos a frente de uma vez.
 */
@Service
@RequiredArgsConstructor
public class ReservaRecorrenteService {

    private static final int HORIZONTE_PADRAO_SEMANAS = 12;

    private final ReservaRecorrenteRepository reservaRecorrenteRepository;
    private final ReservaRepository reservaRepository;
    private final QuadraRepository quadraRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    public ReservaRecorrenteResponse criar(ReservaRecorrenteRequest request) {
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("O horário de término deve ser posterior ao de início.");
        }

        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada: " + request.quadraId()));
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + request.clienteId()));

        ReservaRecorrente recorrencia = ReservaRecorrente.builder()
                .quadra(quadra)
                .cliente(cliente)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .vigenciaInicio(request.vigenciaInicio())
                .vigenciaFim(request.vigenciaFim())
                .ativa(true)
                .build();
        recorrencia = reservaRecorrenteRepository.save(recorrencia);

        LocalDate limite = request.vigenciaFim() != null
                ? request.vigenciaFim()
                : request.vigenciaInicio().plusWeeks(HORIZONTE_PADRAO_SEMANAS);

        LocalDate primeiraData = request.vigenciaInicio().with(TemporalAdjusters.nextOrSame(request.diaSemana()));

        List<LocalDate> datasCriadas = new ArrayList<>();
        List<LocalDate> datasComConflito = new ArrayList<>();

        BigDecimal horas = BigDecimal.valueOf(Duration.between(request.horaInicio(), request.horaFim()).toMinutes())
                .divide(BigDecimal.valueOf(60));
        BigDecimal valorPorOcorrencia = quadra.getValorHora().multiply(horas);

        for (LocalDate data = primeiraData; !data.isAfter(limite); data = data.plusWeeks(1)) {
            LocalDateTime inicio = LocalDateTime.of(data, request.horaInicio());
            LocalDateTime fim = LocalDateTime.of(data, request.horaFim());

            boolean conflito = !reservaRepository.findConflitantes(quadra.getId(), inicio, fim).isEmpty();
            if (conflito) {
                datasComConflito.add(data);
                continue;
            }

            Reserva reserva = Reserva.builder()
                    .quadra(quadra)
                    .cliente(cliente)
                    .inicio(inicio)
                    .fim(fim)
                    .status(StatusReserva.CONFIRMADA)
                    .origem(OrigemReserva.RECEPCAO)
                    .reservaRecorrenteId(recorrencia.getId())
                    .valorTotal(valorPorOcorrencia)
                    .build();
            reservaRepository.save(reserva);
            datasCriadas.add(data);
        }

        return paraResponse(recorrencia, datasCriadas, datasComConflito);
    }

    @Transactional(readOnly = true)
    public List<ReservaRecorrenteResponse> listarAtivas() {
        return reservaRecorrenteRepository.findByAtivaTrue().stream()
                .map(r -> paraResponse(r, List.of(), List.of()))
                .toList();
    }

    /**
     * Desativa a recorrencia e cancela as reservas futuras ja geradas por ela
     * (reservas passadas/concluidas nao sao mexidas).
     */
    @Transactional
    public void cancelar(Long id) {
        ReservaRecorrente recorrencia = reservaRecorrenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recorrência não encontrada: " + id));
        recorrencia.setAtiva(false);
        reservaRecorrenteRepository.save(recorrencia);

        LocalDateTime agora = LocalDateTime.now();
        List<Reserva> futuras = reservaRepository.findByReservaRecorrenteId(id).stream()
                .filter(r -> r.getInicio().isAfter(agora) && r.getStatus() == StatusReserva.CONFIRMADA)
                .toList();

        futuras.forEach(r -> r.setStatus(StatusReserva.CANCELADA));
        reservaRepository.saveAll(futuras);
    }

    /**
     * Estende o horizonte de geracao das recorrencias ativas "sem data final"
     * (vigenciaFim == null): gera as proximas ocorrencias que ainda faltam
     * entre a ultima reserva ja existente e hoje + HORIZONTE_PADRAO_SEMANAS.
     * Chamado periodicamente por ReservaRecorrenteScheduler - sem isso, uma
     * recorrencia sem data final parava de gerar novas reservas apos as 12
     * semanas iniciais (ver HORIZONTE_PADRAO_SEMANAS).
     * Idempotente: so cria ocorrencias cuja data ainda nao existe para essa
     * recorrencia, entao pode ser chamado quantas vezes for preciso.
     */
    @Transactional
    public void estenderHorizonte() {
        LocalDate limiteDesejado = LocalDate.now().plusWeeks(HORIZONTE_PADRAO_SEMANAS);

        for (ReservaRecorrente recorrencia : reservaRecorrenteRepository.findByAtivaTrueAndVigenciaFimIsNull()) {
            List<Reserva> existentes = reservaRepository.findByReservaRecorrenteId(recorrencia.getId());

            LocalDate ultimaData = existentes.stream()
                    .map(r -> r.getInicio().toLocalDate())
                    .max(LocalDate::compareTo)
                    .orElse(recorrencia.getVigenciaInicio().minusWeeks(1));

            Quadra quadra = recorrencia.getQuadra();
            Cliente cliente = recorrencia.getCliente();

            BigDecimal horas = BigDecimal.valueOf(
                            Duration.between(recorrencia.getHoraInicio(), recorrencia.getHoraFim()).toMinutes())
                    .divide(BigDecimal.valueOf(60));
            BigDecimal valorPorOcorrencia = quadra.getValorHora().multiply(horas);

            for (LocalDate data = ultimaData.plusWeeks(1); !data.isAfter(limiteDesejado); data = data.plusWeeks(1)) {
                LocalDateTime inicio = LocalDateTime.of(data, recorrencia.getHoraInicio());
                LocalDateTime fim = LocalDateTime.of(data, recorrencia.getHoraFim());

                boolean conflito = !reservaRepository.findConflitantes(quadra.getId(), inicio, fim).isEmpty();
                if (conflito) {
                    continue;
                }

                Reserva reserva = Reserva.builder()
                        .quadra(quadra)
                        .cliente(cliente)
                        .inicio(inicio)
                        .fim(fim)
                        .status(StatusReserva.CONFIRMADA)
                        .origem(OrigemReserva.RECEPCAO)
                        .reservaRecorrenteId(recorrencia.getId())
                        .valorTotal(valorPorOcorrencia)
                        .build();
                reservaRepository.save(reserva);
            }
        }
    }

    private ReservaRecorrenteResponse paraResponse(ReservaRecorrente r, List<LocalDate> criadas, List<LocalDate> conflitos) {
        return new ReservaRecorrenteResponse(
                r.getId(),
                r.getQuadra().getNome(),
                r.getCliente().getNome(),
                r.getDiaSemana().name(),
                r.getHoraInicio().toString(),
                r.getHoraFim().toString(),
                r.getVigenciaInicio(),
                r.getVigenciaFim(),
                r.isAtiva(),
                criadas,
                conflitos
        );
    }
}
