package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.matriculacliente.MatriculaCliente;
import com.aracabeach.domain.matriculacliente.MatriculaClienteHorario;
import com.aracabeach.domain.matriculacliente.PagamentoMatriculaCliente;
import com.aracabeach.domain.matriculacliente.ReservaGeradaPorMatricula;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.MatriculaClienteHorarioRequest;
import com.aracabeach.dto.MatriculaClienteHorarioResponse;
import com.aracabeach.dto.MatriculaClienteRequest;
import com.aracabeach.dto.MatriculaClienteResponse;
import com.aracabeach.dto.PagamentoMatriculaClienteResponse;
import com.aracabeach.dto.RegistrarPagamentoMatriculaClienteRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.MatriculaClienteHorarioRepository;
import com.aracabeach.repository.MatriculaClienteRepository;
import com.aracabeach.repository.PagamentoMatriculaClienteRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaGeradaPorMatriculaRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Matricula de cliente com mensalidade unica cobrindo VARIOS horarios
 * semanais de quadra (ver MatriculaCliente). Pensada para a pagina de
 * Mensalidades: em vez de precisar criar uma ReservaRecorrente por dia da
 * semana e depois uma Mensalidade pra cada uma, o usuario cadastra a
 * matricula uma vez, com a lista de dias/horarios, e ja sai com uma unica
 * cobranca mensal cobrindo tudo.
 *
 * E um fluxo PARALELO a ReservaRecorrenteService/MensalidadeService (nao
 * reaproveita essas tabelas, para nao arriscar o que ja esta em producao),
 * mas gera Reserva normalmente (mesma tabela da agenda) para cada
 * ocorrencia, entao as aulas/jogos aparecem na Agenda normalmente - so a
 * cobranca que e separada e combinada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatriculaClienteService {

    private static final int HORIZONTE_PADRAO_SEMANAS = 12;

    private final MatriculaClienteRepository matriculaClienteRepository;
    private final MatriculaClienteHorarioRepository horarioRepository;
    private final ReservaGeradaPorMatriculaRepository reservaGeradaRepository;
    private final PagamentoMatriculaClienteRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final QuadraRepository quadraRepository;
    private final ReservaRepository reservaRepository;
    private final com.aracabeach.repository.BloqueioQuadraRepository bloqueioQuadraRepository;
    private final PagamentoRepository pagamentoCaixaRepository;

    @Transactional
    public MatriculaClienteResponse criar(MatriculaClienteRequest request) {
        for (MatriculaClienteHorarioRequest h : request.horarios()) {
            if (!h.horaFim().isAfter(h.horaInicio())) {
                throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio em todos os horarios.");
            }
        }

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + request.clienteId()));

        MatriculaCliente matricula = MatriculaCliente.builder()
                .cliente(cliente)
                .valorMensal(request.valorMensal())
                .diaVencimento(request.diaVencimento())
                .dataInicio(request.dataInicio())
                .ativa(true)
                .build();
        matricula = matriculaClienteRepository.save(matricula);

        int totalGeradas = 0;
        int totalConflitos = 0;

        for (MatriculaClienteHorarioRequest hReq : request.horarios()) {
            Quadra quadra = quadraRepository.findById(hReq.quadraId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + hReq.quadraId()));

            MatriculaClienteHorario horario = MatriculaClienteHorario.builder()
                    .matriculaCliente(matricula)
                    .quadra(quadra)
                    .diaSemana(hReq.diaSemana())
                    .horaInicio(hReq.horaInicio())
                    .horaFim(hReq.horaFim())
                    .ativo(true)
                    .build();
            horario = horarioRepository.save(horario);

            int[] resultado = gerarOcorrencias(horario, request.dataInicio(),
                    request.dataInicio().plusWeeks(HORIZONTE_PADRAO_SEMANAS));
            totalGeradas += resultado[0];
            totalConflitos += resultado[1];
        }

        String resumo = totalGeradas + " aula(s) agendada(s) nas próximas " + HORIZONTE_PADRAO_SEMANAS + " semanas"
                + (totalConflitos > 0 ? ", " + totalConflitos + " data(s) com conflito foram puladas." : ".");

        return paraResponse(matricula, resumo);
    }

    /**
     * Gera as ocorrencias de Reserva de um horario entre [desde, ate),
     * pulando datas com conflito (quadra ja ocupada por outra reserva) e
     * registrando cada uma criada em ReservaGeradaPorMatricula. Retorna
     * [quantidadeGeradas, quantidadeComConflito].
     */
    private int[] gerarOcorrencias(MatriculaClienteHorario horario, LocalDate desde, LocalDate ate) {
        Quadra quadra = horario.getQuadra();
        BigDecimal horas = BigDecimal.valueOf(Duration.between(horario.getHoraInicio(), horario.getHoraFim()).toMinutes())
                .divide(BigDecimal.valueOf(60), 6, java.math.RoundingMode.HALF_UP);
        // Zero porque a cobranca e feita pela matricula combinada (mensal), nao por sessao.
        BigDecimal valorPorOcorrencia = BigDecimal.ZERO;

        int geradas = 0;
        int comConflito = 0;

        LocalDate primeiraData = desde.with(TemporalAdjusters.nextOrSame(horario.getDiaSemana()));
        for (LocalDate data = primeiraData; data.isBefore(ate); data = data.plusWeeks(1)) {
            LocalDateTime inicio = LocalDateTime.of(data, horario.getHoraInicio());
            LocalDateTime fim = LocalDateTime.of(data, horario.getHoraFim());

            boolean conflito = !reservaRepository.findConflitantes(quadra.getId(), inicio, fim).isEmpty()
                    || !bloqueioQuadraRepository.findConflitantes(quadra.getId(), inicio, fim).isEmpty();
            if (conflito) {
                comConflito++;
                continue;
            }

            Reserva reserva = Reserva.builder()
                    .quadra(quadra)
                    .cliente(horario.getMatriculaCliente().getCliente())
                    .inicio(inicio)
                    .fim(fim)
                    .status(StatusReserva.CONFIRMADA)
                    .origem(OrigemReserva.RECEPCAO)
                    .valorTotal(valorPorOcorrencia)
                    .observacoes("Matrícula #" + horario.getMatriculaCliente().getId() + " (mensalidade combinada)")
                    .build();
            reserva = reservaRepository.save(reserva);

            reservaGeradaRepository.save(ReservaGeradaPorMatricula.builder()
                    .horario(horario)
                    .reservaId(reserva.getId())
                    .data(data)
                    .build());
            geradas++;
        }

        return new int[]{geradas, comConflito};
    }

    /**
     * Estende o horizonte de geracao de todos os horarios ativos (mesmo
     * racional de ReservaRecorrenteService.estenderHorizonte). Chamado
     * periodicamente por MatriculaClienteScheduler.
     */
    @Transactional
    public void estenderHorizonte() {
        LocalDate limiteDesejado = LocalDate.now().plusWeeks(HORIZONTE_PADRAO_SEMANAS);

        for (MatriculaClienteHorario horario : horarioRepository.findByAtivoTrue()) {
            if (!horario.getMatriculaCliente().isAtiva()) {
                continue;
            }

            List<ReservaGeradaPorMatricula> existentes = reservaGeradaRepository.findByHorarioIdOrderByDataDesc(horario.getId());
            LocalDate ultimaData = existentes.isEmpty()
                    ? horario.getMatriculaCliente().getDataInicio().minusWeeks(1)
                    : existentes.get(0).getData();

            if (!ultimaData.plusWeeks(1).isBefore(limiteDesejado)) {
                continue;
            }

            gerarOcorrencias(horario, ultimaData.plusWeeks(1), limiteDesejado);
        }
    }

    @Transactional(readOnly = true)
    public List<MatriculaClienteResponse> listarAtivas() {
        return matriculaClienteRepository.findByAtivaTrue().stream()
                .map(m -> paraResponse(m, null))
                .toList();
    }

    @Transactional
    public void desativarMatricula(Long id) {
        MatriculaCliente matricula = matriculaClienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matricula nao encontrada: " + id));
        matricula.setAtiva(false);
        matriculaClienteRepository.save(matricula);

        for (MatriculaClienteHorario horario : horarioRepository.findByMatriculaClienteId(id)) {
            desativarHorarioInterno(horario);
        }
    }

    @Transactional
    public void desativarHorario(Long horarioId) {
        MatriculaClienteHorario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Horario nao encontrado: " + horarioId));
        desativarHorarioInterno(horario);
    }

    private void desativarHorarioInterno(MatriculaClienteHorario horario) {
        horario.setAtivo(false);
        horarioRepository.save(horario);

        LocalDateTime agora = LocalDateTime.now();
        List<Long> idsReservasFuturas = reservaGeradaRepository
                .findByHorarioIdAndDataAfter(horario.getId(), LocalDate.now().minusDays(1)).stream()
                .map(ReservaGeradaPorMatricula::getReservaId)
                .toList();

        List<Reserva> reservas = reservaRepository.findAllById(idsReservasFuturas).stream()
                .filter(r -> r.getInicio().isAfter(agora) && r.getStatus() == StatusReserva.CONFIRMADA)
                .toList();
        reservas.forEach(r -> r.setStatus(StatusReserva.CANCELADA));
        reservaRepository.saveAll(reservas);
    }

    @Transactional(readOnly = true)
    public List<PagamentoMatriculaClienteResponse> listarPagamentos(Long matriculaId) {
        return pagamentoRepository.findByMatriculaClienteIdOrderByVencimentoDesc(matriculaId).stream()
                .map(this::paraPagamentoResponse)
                .toList();
    }

    @Transactional
    public PagamentoMatriculaClienteResponse registrarPagamento(Long pagamentoId, RegistrarPagamentoMatriculaClienteRequest request) {
        PagamentoMatriculaCliente pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobranca de matricula nao encontrada: " + pagamentoId));

        if (pagamento.isPago()) {
            throw new IllegalArgumentException("Essa cobranca ja foi paga.");
        }

        pagamento.setPago(true);
        pagamento.setDataPagamento(LocalDate.now());
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamento = pagamentoRepository.save(pagamento);

        Pagamento pagamentoCaixa = Pagamento.builder()
                .reserva(null)
                .valor(pagamento.getValor())
                .formaPagamento(pagamento.getFormaPagamento())
                .status(StatusPagamento.PAGO)
                .ehSinal(false)
                .build();
        pagamentoCaixaRepository.save(pagamentoCaixa);

        return paraPagamentoResponse(pagamento);
    }

    /**
     * Mesmo padrao idempotente de MensalidadeService.gerarCobrancasDoMes().
     */
    @Transactional
    public int gerarCobrancasDoMes() {
        YearMonth mesAtual = YearMonth.now();
        String mesAtualStr = mesAtual.toString();
        int geradas = 0;

        for (MatriculaCliente matricula : matriculaClienteRepository.findByAtivaTrue()) {
            if (mesAtualStr.equals(matricula.getUltimoMesGerado())) {
                continue;
            }

            Optional<PagamentoMatriculaCliente> existente =
                    pagamentoRepository.findByMatriculaClienteIdAndReferenciaMes(matricula.getId(), mesAtualStr);
            if (existente.isPresent()) {
                matricula.setUltimoMesGerado(mesAtualStr);
                matriculaClienteRepository.save(matricula);
                continue;
            }

            int dia = Math.min(matricula.getDiaVencimento(), mesAtual.lengthOfMonth());
            LocalDate vencimento = mesAtual.atDay(dia);

            PagamentoMatriculaCliente cobranca = PagamentoMatriculaCliente.builder()
                    .matriculaCliente(matricula)
                    .referenciaMes(mesAtualStr)
                    .valor(matricula.getValorMensal())
                    .vencimento(vencimento)
                    .pago(false)
                    .build();
            pagamentoRepository.save(cobranca);

            matricula.setUltimoMesGerado(mesAtualStr);
            matriculaClienteRepository.save(matricula);
            geradas++;
        }

        if (geradas > 0) {
            log.info("{} cobrança(s) de matrícula de cliente geradas para o mês {}.", geradas, mesAtualStr);
        }
        return geradas;
    }

    private MatriculaClienteResponse paraResponse(MatriculaCliente m, String resumoGeracao) {
        List<MatriculaClienteHorarioResponse> horarios = horarioRepository.findByMatriculaClienteId(m.getId()).stream()
                .filter(MatriculaClienteHorario::isAtivo)
                .map(h -> new MatriculaClienteHorarioResponse(
                        h.getId(), h.getQuadra().getId(), h.getQuadra().getNome(),
                        h.getDiaSemana().name(), h.getHoraInicio().toString(), h.getHoraFim().toString(), h.isAtivo()))
                .toList();

        return new MatriculaClienteResponse(
                m.getId(), m.getCliente().getId(), m.getCliente().getNome(),
                m.getValorMensal(), m.getDiaVencimento(), m.getDataInicio(), m.isAtiva(),
                horarios, resumoGeracao
        );
    }

    private PagamentoMatriculaClienteResponse paraPagamentoResponse(PagamentoMatriculaCliente p) {
        return new PagamentoMatriculaClienteResponse(
                p.getId(), p.getMatriculaCliente().getId(), p.getMatriculaCliente().getCliente().getNome(),
                p.getReferenciaMes(), p.getValor(), p.getVencimento(), p.isPago(), p.getDataPagamento(), p.getFormaPagamento()
        );
    }
}
