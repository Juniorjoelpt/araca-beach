package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.comissao.OrigemComissao;
import com.aracabeach.domain.financeiro.FormaPagamento;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.pacote.AulaPacote;
import com.aracabeach.domain.pacote.PacoteCliente;
import com.aracabeach.domain.pacote.PlanoPacote;
import com.aracabeach.domain.pacote.StatusAulaPacote;
import com.aracabeach.domain.professor.Professor;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.regra.ConfiguracaoReserva;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.AgendarAulaPacoteRequest;
import com.aracabeach.dto.AulaPacoteResponse;
import com.aracabeach.dto.PacoteClienteResponse;
import com.aracabeach.dto.PlanoPacoteRequest;
import com.aracabeach.dto.VenderPacoteRequest;
import com.aracabeach.exception.ConflitoHorarioException;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.AulaPacoteRepository;
import com.aracabeach.repository.AulaRepository;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.PacoteClienteRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.PlanoPacoteRepository;
import com.aracabeach.repository.ProfessorRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pacotes de aulas: planos vendidos, saldo de creditos, agendamento (que
 * reserva a quadra), chamada/presenca, reposicao e comissao do professor.
 *
 * Regra de saldo: AGENDADA, REALIZADA e FALTA_SEM_AVISO consomem credito;
 * FALTA_AVISADA e CANCELADA devolvem o credito, permitindo agendar a
 * reposicao.
 */
@Service
@RequiredArgsConstructor
public class PacoteService {

    private final PlanoPacoteRepository planoRepository;
    private final PacoteClienteRepository pacoteRepository;
    private final AulaPacoteRepository aulaPacoteRepository;
    private final ClienteRepository clienteRepository;
    private final ProfessorRepository professorRepository;
    private final QuadraRepository quadraRepository;
    private final ReservaRepository reservaRepository;
    private final AulaRepository aulaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ReservaService reservaService;
    private final ComissaoService comissaoService;
    private final ConfiguracaoReservaService configuracaoService;

    // ---- Planos ----

    @Transactional(readOnly = true)
    public List<PlanoPacote> listarPlanos(boolean somenteAtivos) {
        return somenteAtivos ? planoRepository.findByAtivoTrueOrderByNomeAsc() : planoRepository.findAll();
    }

    @Transactional
    public PlanoPacote criarPlano(PlanoPacoteRequest r) {
        return planoRepository.save(aplicar(new PlanoPacote(), r));
    }

    @Transactional
    public PlanoPacote atualizarPlano(Long id, PlanoPacoteRequest r) {
        PlanoPacote plano = planoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano nao encontrado: " + id));
        return planoRepository.save(aplicar(plano, r));
    }

    private PlanoPacote aplicar(PlanoPacote p, PlanoPacoteRequest r) {
        p.setNome(r.nome());
        p.setTipo(r.tipo() == null || r.tipo().isBlank() ? null : r.tipo());
        p.setQuantidadeAulas(r.quantidadeAulas());
        p.setValor(r.valor());
        p.setValidadeDias(r.validadeDias());
        p.setAtivo(r.ativo());
        return p;
    }

    // ---- Vendas ----

    @Transactional
    public PacoteClienteResponse vender(VenderPacoteRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + request.clienteId()));
        PlanoPacote plano = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano nao encontrado: " + request.planoId()));
        if (!plano.isAtivo()) {
            throw new IllegalArgumentException("Esse plano esta inativo.");
        }
        LocalDate hoje = LocalDate.now();
        PacoteCliente pacote = pacoteRepository.save(PacoteCliente.builder()
                .cliente(cliente)
                .plano(plano)
                .aulasTotal(plano.getQuantidadeAulas())
                .valor(plano.getValor())
                .dataCompra(hoje)
                .validade(hoje.plusDays(plano.getValidadeDias()))
                .build());
        if (request.formaPagamento() != null) {
            registrarPagamento(pacote, request.formaPagamento());
        }
        return paraResponse(pacote);
    }

    @Transactional
    public PacoteClienteResponse pagar(Long pacoteId, FormaPagamento forma) {
        PacoteCliente pacote = buscar(pacoteId);
        if (pacote.isPago()) {
            throw new IllegalArgumentException("Esse pacote ja foi pago.");
        }
        if (pacote.isCancelado()) {
            throw new IllegalArgumentException("Esse pacote foi cancelado.");
        }
        registrarPagamento(pacote, forma);
        return paraResponse(pacote);
    }

    private void registrarPagamento(PacoteCliente pacote, FormaPagamento forma) {
        pacote.setPago(true);
        pacote.setDataPagamento(LocalDate.now());
        pacote.setFormaPagamento(forma);
        pacoteRepository.save(pacote);
        // Mesma integracao das mensalidades: entra no fechamento de caixa.
        pagamentoRepository.save(Pagamento.builder()
                .reserva(null)
                .valor(pacote.getValor())
                .formaPagamento(forma)
                .status(StatusPagamento.PAGO)
                .ehSinal(false)
                .build());
    }

    /** Cancela o pacote (sem estorno automatico) e as aulas ainda agendadas. */
    @Transactional
    public PacoteClienteResponse cancelar(Long pacoteId) {
        PacoteCliente pacote = buscar(pacoteId);
        pacote.setCancelado(true);
        pacoteRepository.save(pacote);
        for (AulaPacote aula : aulaPacoteRepository.findByPacoteIdOrderByInicioAsc(pacoteId)) {
            if (aula.getStatus() == StatusAulaPacote.AGENDADA) {
                aula.setStatus(StatusAulaPacote.CANCELADA);
                liberarQuadra(aula);
                aulaPacoteRepository.save(aula);
            }
        }
        return paraResponse(pacote);
    }

    @Transactional(readOnly = true)
    public List<PacoteClienteResponse> listar(Long clienteId) {
        List<PacoteCliente> pacotes = clienteId != null
                ? pacoteRepository.findByClienteIdOrderByDataCompraDescIdDesc(clienteId)
                : pacoteRepository.findAllByOrderByDataCompraDescIdDesc();
        return pacotes.stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PacoteClienteResponse obter(Long id) {
        return paraResponse(buscar(id));
    }

    // ---- Aulas / chamada ----

    @Transactional(readOnly = true)
    public List<AulaPacoteResponse> listarAulasDoPacote(Long pacoteId) {
        return aulaPacoteRepository.findByPacoteIdOrderByInicioAsc(pacoteId).stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AulaPacoteResponse> listarAulasDoDia(LocalDate data) {
        return aulaPacoteRepository.findByInicioBetweenOrderByInicioAsc(
                data.atStartOfDay(), data.plusDays(1).atStartOfDay()).stream().map(this::paraResponse).toList();
    }

    @Transactional
    public AulaPacoteResponse agendarAula(Long pacoteId, AgendarAulaPacoteRequest r) {
        PacoteCliente pacote = buscar(pacoteId);
        if (pacote.isCancelado()) {
            throw new IllegalArgumentException("Esse pacote foi cancelado.");
        }
        if (!r.fim().isAfter(r.inicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }
        if (r.inicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Nao e possivel agendar aula no passado.");
        }
        if (r.inicio().toLocalDate().isAfter(pacote.getValidade())) {
            throw new IllegalArgumentException("O pacote vence em " + pacote.getValidade() + ": a aula precisa ser ate essa data.");
        }
        if (saldo(pacote) <= 0) {
            throw new IllegalArgumentException("O pacote nao tem saldo de aulas.");
        }
        if (r.reposicaoDeAulaId() != null) {
            AulaPacote original = aulaPacoteRepository.findById(r.reposicaoDeAulaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Aula original nao encontrada."));
            if (!original.getPacote().getId().equals(pacoteId)
                    || original.getStatus() != StatusAulaPacote.FALTA_AVISADA) {
                throw new IllegalArgumentException("A aula original precisa ser uma falta avisada deste pacote.");
            }
        }

        Professor professor = professorRepository.findById(r.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + r.professorId()));
        Quadra quadra = quadraRepository.findById(r.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + r.quadraId()));

        boolean professorOcupado = !aulaRepository.findConflitantesDoProfessor(professor.getId(), r.inicio(), r.fim()).isEmpty()
                || !aulaPacoteRepository.findConflitantesDoProfessor(professor.getId(), r.inicio(), r.fim()).isEmpty();
        if (professorOcupado) {
            throw new ConflitoHorarioException("O professor ja tem uma aula nesse horario.");
        }

        // Reserva da quadra (valor zero: o pacote ja foi cobrado). Valida bloqueio/conflito.
        reservaService.validarHorarioReservavel(quadra, r.inicio(), r.fim());
        reservaService.validarDisponibilidade(quadra.getId(), r.inicio(), r.fim(), null);
        Reserva reserva = reservaRepository.save(Reserva.builder()
                .quadra(quadra)
                .cliente(pacote.getCliente())
                .inicio(r.inicio())
                .fim(r.fim())
                .status(StatusReserva.CONFIRMADA)
                .origem(OrigemReserva.AULA)
                .valorTotal(BigDecimal.ZERO)
                .observacoes("Aula do pacote #" + pacote.getId() + " - " + professor.getNome())
                .build());

        AulaPacote aula = aulaPacoteRepository.save(AulaPacote.builder()
                .pacote(pacote)
                .professor(professor)
                .quadra(quadra)
                .reserva(reserva)
                .inicio(r.inicio())
                .fim(r.fim())
                .reposicaoDeAulaId(r.reposicaoDeAulaId())
                .observacoes(r.observacoes())
                .build());
        return paraResponse(aula);
    }

    /** Chamada feita pela equipe. */
    @Transactional
    public AulaPacoteResponse registrarPresenca(Long aulaId, StatusAulaPacote novoStatus) {
        AulaPacote aula = buscarAula(aulaId);
        if (aula.getStatus() != StatusAulaPacote.AGENDADA) {
            throw new IllegalArgumentException("A chamada dessa aula ja foi registrada (" + aula.getStatus() + ").");
        }
        if (novoStatus == StatusAulaPacote.AGENDADA) {
            throw new IllegalArgumentException("Status invalido para a chamada.");
        }
        if ((novoStatus == StatusAulaPacote.REALIZADA || novoStatus == StatusAulaPacote.FALTA_SEM_AVISO)
                && aula.getInicio().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("A aula ainda nao aconteceu.");
        }
        return aplicarStatus(aula, novoStatus);
    }

    /** O proprio aluno avisa que vai faltar (portal): ate X horas antes devolve o credito. */
    @Transactional
    public AulaPacoteResponse avisarFalta(Long aulaId, Long clienteId) {
        AulaPacote aula = buscarAula(aulaId);
        if (!aula.getPacote().getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Esta aula nao pertence a voce.");
        }
        if (aula.getStatus() != StatusAulaPacote.AGENDADA) {
            throw new IllegalArgumentException("Essa aula nao esta mais agendada.");
        }
        if (!aula.getInicio().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Essa aula ja comecou.");
        }
        ConfiguracaoReserva cfg = configuracaoService.obter();
        boolean noPrazo = !LocalDateTime.now().isAfter(aula.getInicio().minusHours(cfg.getHorasAvisoFaltaAula()));
        // Fora do prazo a aula segue AGENDADA e a equipe decide na chamada.
        if (!noPrazo) {
            throw new IllegalArgumentException("O aviso de falta precisa ser feito com pelo menos "
                    + cfg.getHorasAvisoFaltaAula() + "h de antecedencia. Fale com a recepcao.");
        }
        return aplicarStatus(aula, StatusAulaPacote.FALTA_AVISADA);
    }

    private AulaPacoteResponse aplicarStatus(AulaPacote aula, StatusAulaPacote status) {
        aula.setStatus(status);
        if (!status.consomeCredito()) {
            liberarQuadra(aula);
        }
        if (status == StatusAulaPacote.REALIZADA) {
            reservaConcluida(aula);
        }
        aulaPacoteRepository.save(aula);

        if (status == StatusAulaPacote.REALIZADA || status == StatusAulaPacote.FALTA_SEM_AVISO) {
            PacoteCliente pacote = aula.getPacote();
            BigDecimal valorPorAula = pacote.getValor()
                    .divide(BigDecimal.valueOf(Math.max(1, pacote.getAulasTotal())), 2, RoundingMode.HALF_UP);
            comissaoService.lancar(aula.getProfessor(), OrigemComissao.AULA_PACOTE, aula.getId(),
                    "Aula de pacote - " + pacote.getCliente().getNome() + " - " + aula.getInicio().toLocalDate(),
                    aula.getInicio().toLocalDate(), valorPorAula);
        }
        return paraResponse(aula);
    }

    private void liberarQuadra(AulaPacote aula) {
        Reserva reserva = aula.getReserva();
        if (reserva != null && reserva.getStatus() == StatusReserva.CONFIRMADA) {
            reserva.setStatus(StatusReserva.CANCELADA);
            reserva.setCanceladaEm(LocalDateTime.now());
            reservaRepository.save(reserva);
        }
    }

    private void reservaConcluida(AulaPacote aula) {
        Reserva reserva = aula.getReserva();
        if (reserva != null && reserva.getStatus() == StatusReserva.CONFIRMADA) {
            reserva.setStatus(StatusReserva.CONCLUIDA);
            reservaRepository.save(reserva);
        }
    }

    // ---- Consultas usadas pelo portal ----

    @Transactional(readOnly = true)
    public List<PacoteClienteResponse> pacotesDoCliente(Long clienteId) {
        return listar(clienteId);
    }

    @Transactional(readOnly = true)
    public List<AulaPacoteResponse> aulasDoCliente(Long clienteId) {
        return aulaPacoteRepository.findByPacoteClienteIdOrderByInicioDesc(clienteId).stream()
                .map(this::paraResponse).toList();
    }

    // ---- util ----

    private int saldo(PacoteCliente pacote) {
        return pacote.getAulasTotal() - (int) aulaPacoteRepository.contarConsumidas(pacote.getId());
    }

    private PacoteCliente buscar(Long id) {
        return pacoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pacote nao encontrado: " + id));
    }

    private AulaPacote buscarAula(Long id) {
        return aulaPacoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula nao encontrada: " + id));
    }

    private PacoteClienteResponse paraResponse(PacoteCliente p) {
        int consumidas = (int) aulaPacoteRepository.contarConsumidas(p.getId());
        int saldo = p.getAulasTotal() - consumidas;
        String situacao;
        if (p.isCancelado()) {
            situacao = "CANCELADO";
        } else if (saldo <= 0) {
            situacao = "ESGOTADO";
        } else if (p.getValidade().isBefore(LocalDate.now())) {
            situacao = "VENCIDO";
        } else {
            situacao = "ATIVO";
        }
        return new PacoteClienteResponse(
                p.getId(), p.getCliente().getId(), p.getCliente().getNome(),
                p.getPlano().getId(), p.getPlano().getNome(), p.getPlano().getTipo(),
                p.getAulasTotal(), consumidas, saldo, p.getValor(), p.getDataCompra(), p.getValidade(),
                p.isPago(), p.getDataPagamento(),
                p.getFormaPagamento() != null ? p.getFormaPagamento().name() : null,
                situacao);
    }

    private AulaPacoteResponse paraResponse(AulaPacote a) {
        return new AulaPacoteResponse(
                a.getId(), a.getPacote().getId(),
                a.getPacote().getCliente().getId(), a.getPacote().getCliente().getNome(),
                a.getPacote().getPlano().getNome(),
                a.getProfessor().getId(), a.getProfessor().getNome(),
                a.getQuadra().getId(), a.getQuadra().getNome(),
                a.getInicio(), a.getFim(), a.getStatus().name(),
                a.getReposicaoDeAulaId(), a.getObservacoes());
    }
}
