package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.matricula.Matricula;
import com.aracabeach.domain.matricula.PagamentoMatricula;
import com.aracabeach.domain.turma.Turma;
import com.aracabeach.dto.MatriculaRequest;
import com.aracabeach.dto.MatriculaResponse;
import com.aracabeach.dto.PagamentoMatriculaResponse;
import com.aracabeach.dto.RegistrarPagamentoMatriculaRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.MatriculaRepository;
import com.aracabeach.repository.PagamentoMatriculaRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.TurmaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Matricula de um cliente numa turma, com cobranca mensal - e o modulo de
 * Matricula vinculado a mensalidade pedido pelo usuario, clonando o padrao
 * ja em producao de MensalidadeService (idempotente via ultimoMesGerado, e
 * o pagamento tambem gera um Pagamento solto para entrar no fechamento de
 * caixa). Mantido como fluxo separado de Mensalidade/ReservaRecorrente
 * porque uma Turma tem VARIOS alunos (Mensalidade e 1 cliente por
 * recorrencia), entao nao da para reaproveitar a mesma tabela sem mexer no
 * que ja esta funcionando.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final PagamentoMatriculaRepository pagamentoMatriculaRepository;
    private final TurmaRepository turmaRepository;
    private final ClienteRepository clienteRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ComissaoService comissaoService;

    @Transactional
    public MatriculaResponse criar(MatriculaRequest request) {
        Turma turma = turmaRepository.findById(request.turmaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma nao encontrada: " + request.turmaId()));
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + request.clienteId()));

        if (!turma.isAtiva()) {
            throw new IllegalArgumentException("Nao e possivel matricular em uma turma inativa.");
        }

        matriculaRepository.findByTurmaIdAndClienteIdAndAtivaTrue(turma.getId(), cliente.getId())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("Este cliente ja esta matriculado nesta turma.");
                });

        if (turma.getCapacidadeMaxima() != null) {
            int matriculadosAtuais = matriculaRepository.findByTurmaIdAndAtivaTrue(turma.getId()).size();
            if (matriculadosAtuais >= turma.getCapacidadeMaxima()) {
                throw new IllegalArgumentException(
                        "A turma ja atingiu a capacidade maxima de " + turma.getCapacidadeMaxima() + " aluno(s).");
            }
        }

        Matricula matricula = Matricula.builder()
                .turma(turma)
                .cliente(cliente)
                .dataMatricula(LocalDate.now())
                .valorMensal(request.valorMensal())
                .diaVencimento(request.diaVencimento())
                .ativa(true)
                .build();
        matricula = matriculaRepository.save(matricula);

        // Gera a cobranca do mes atual na hora, em vez de esperar o scheduler
        // da 1h25 - sem isso, uma matricula criada de tarde so teria cobranca
        // pendente no dia seguinte, e nao dava pra marcar a primeira
        // mensalidade como paga nem na turma nem no financeiro (mesmo ajuste
        // ja feito em MensalidadeService e MatriculaClienteService).
        gerarCobrancaDoMes(matricula, YearMonth.now());

        return paraResponse(matricula);
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarAtivas() {
        return matriculaRepository.findByAtivaTrue().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorTurma(Long turmaId) {
        return matriculaRepository.findByTurmaIdAndAtivaTrue(turmaId).stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorCliente(Long clienteId) {
        return matriculaRepository.findByClienteIdAndAtivaTrue(clienteId).stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional
    public void cancelar(Long id) {
        Matricula matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matricula nao encontrada: " + id));
        matricula.setAtiva(false);
        matriculaRepository.save(matricula);
    }

    @Transactional(readOnly = true)
    public List<PagamentoMatriculaResponse> listarPagamentos(Long matriculaId) {
        return pagamentoMatriculaRepository.findByMatriculaIdOrderByVencimentoDesc(matriculaId).stream()
                .map(this::paraPagamentoResponse)
                .toList();
    }

    @Transactional
    public PagamentoMatriculaResponse registrarPagamento(Long pagamentoId, RegistrarPagamentoMatriculaRequest request) {
        PagamentoMatricula pagamento = pagamentoMatriculaRepository.findById(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobranca de matricula nao encontrada: " + pagamentoId));

        if (pagamento.isPago()) {
            throw new IllegalArgumentException("Essa cobranca ja foi paga.");
        }

        pagamento.setPago(true);
        pagamento.setDataPagamento(LocalDate.now());
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamento = pagamentoMatriculaRepository.save(pagamento);

        // Mesma integracao que PagamentoMensalidade: cria um Pagamento sem
        // reserva vinculada para entrar automaticamente no fechamento de caixa.
        Pagamento pagamentoCaixa = Pagamento.builder()
                .reserva(null)
                .valor(pagamento.getValor())
                .formaPagamento(pagamento.getFormaPagamento())
                .status(StatusPagamento.PAGO)
                .ehSinal(false)
                .build();
        pagamentoRepository.save(pagamentoCaixa);

        Turma turma = pagamento.getMatricula().getTurma();
        comissaoService.lancar(turma.getProfessor(), com.aracabeach.domain.comissao.OrigemComissao.MATRICULA,
                pagamento.getId(), "Mensalidade " + turma.getNome() + " " + pagamento.getReferenciaMes(),
                pagamento.getDataPagamento(), pagamento.getValor());

        return paraPagamentoResponse(pagamento);
    }

    /**
     * Para cada matricula ativa que ainda nao gerou a cobranca do mes atual,
     * cria a PagamentoMatricula pendente. Idempotente via ultimoMesGerado -
     * mesmo padrao de MensalidadeService.gerarCobrancasDoMes().
     */
    @Transactional
    public int gerarCobrancasDoMes() {
        YearMonth mesAtual = YearMonth.now();
        int geradas = 0;

        for (Matricula matricula : matriculaRepository.findByAtivaTrue()) {
            if (gerarCobrancaDoMes(matricula, mesAtual)) {
                geradas++;
            }
        }

        if (geradas > 0) {
            log.info("{} cobrança(s) de matrícula geradas para o mês {}.", geradas, mesAtual);
        }
        return geradas;
    }

    /**
     * Gera (se ainda nao existir) a cobranca de uma matricula para o mes
     * informado. Usado tanto pelo job diario (gerarCobrancasDoMes, para
     * todas as matriculas ativas) quanto na criacao da matricula (para nao
     * esperar o proximo ciclo do scheduler so para ter a primeira cobranca).
     * Retorna true se uma cobranca nova foi criada.
     */
    private boolean gerarCobrancaDoMes(Matricula matricula, YearMonth mes) {
        String mesStr = mes.toString();

        if (mesStr.equals(matricula.getUltimoMesGerado())) {
            return false;
        }

        Optional<PagamentoMatricula> existente =
                pagamentoMatriculaRepository.findByMatriculaIdAndReferenciaMes(matricula.getId(), mesStr);
        if (existente.isPresent()) {
            matricula.setUltimoMesGerado(mesStr);
            matriculaRepository.save(matricula);
            return false;
        }

        int dia = Math.min(matricula.getDiaVencimento(), mes.lengthOfMonth());
        LocalDate vencimento = mes.atDay(dia);

        PagamentoMatricula cobranca = PagamentoMatricula.builder()
                .matricula(matricula)
                .referenciaMes(mesStr)
                .valor(matricula.getValorMensal())
                .vencimento(vencimento)
                .pago(false)
                .build();
        pagamentoMatriculaRepository.save(cobranca);

        matricula.setUltimoMesGerado(mesStr);
        matriculaRepository.save(matricula);
        return true;
    }

    private MatriculaResponse paraResponse(Matricula m) {
        return new MatriculaResponse(
                m.getId(),
                m.getTurma().getId(),
                m.getTurma().getNome(),
                m.getCliente().getId(),
                m.getCliente().getNome(),
                m.getDataMatricula(),
                m.getValorMensal(),
                m.getDiaVencimento(),
                m.isAtiva()
        );
    }

    private PagamentoMatriculaResponse paraPagamentoResponse(PagamentoMatricula p) {
        return new PagamentoMatriculaResponse(
                p.getId(),
                p.getMatricula().getId(),
                p.getMatricula().getCliente().getNome(),
                p.getMatricula().getTurma().getNome(),
                p.getReferenciaMes(),
                p.getValor(),
                p.getVencimento(),
                p.isPago(),
                p.getDataPagamento(),
                p.getFormaPagamento()
        );
    }
}
