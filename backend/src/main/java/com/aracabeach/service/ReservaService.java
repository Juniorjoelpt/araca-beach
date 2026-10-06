package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.quadra.StatusQuadra;
import com.aracabeach.domain.regra.ConfiguracaoReserva;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.PoliticaCancelamentoResponse;
import com.aracabeach.dto.PrecoResponse;
import com.aracabeach.dto.ReservaRequest;
import com.aracabeach.exception.ConflitoHorarioException;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.BloqueioQuadraRepository;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negocio do agendamento de quadras: conflito de horario, bloqueio
 * de quadra, preco por horario (+ desconto de mensalista), politica de
 * cancelamento com multa, nao comparecimento e lista de espera.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AuditoriaService auditoria;
    private final QuadraRepository quadraRepository;
    private final ClienteRepository clienteRepository;
    private final NotificacaoService notificacaoService;
    private final BloqueioQuadraRepository bloqueioQuadraRepository;
    private final PrecificacaoService precificacaoService;
    private final ConfiguracaoReservaService configuracaoService;
    private final ListaEsperaService listaEsperaService;

    @Transactional
    public Reserva criar(ReservaRequest request) {
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }

        if (request.inicio().toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Nao e possivel criar uma reserva em uma data passada.");
        }

        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + request.clienteId()));

        return criarReserva(quadra, cliente, request.inicio(), request.fim(),
                OrigemReserva.RECEPCAO, request.observacoes());
    }

    /**
     * Nucleo da criacao de reserva, compartilhado com o portal do cliente:
     * valida quadra (status), bloqueios e conflitos, calcula o preco e
     * dispara a confirmacao.
     */
    @Transactional
    public Reserva criarReserva(Quadra quadra, Cliente cliente, LocalDateTime inicio, LocalDateTime fim,
                                OrigemReserva origem, String observacoes) {
        validarHorarioReservavel(quadra, inicio, fim);
        validarDisponibilidade(quadra.getId(), inicio, fim, null);

        PrecoResponse preco = precificacaoService.calcular(quadra, inicio, fim, cliente.getId());

        Reserva reserva = Reserva.builder()
                .quadra(quadra)
                .cliente(cliente)
                .inicio(inicio)
                .fim(fim)
                .status(StatusReserva.CONFIRMADA)
                .origem(origem)
                .valorTotal(preco.valorTotal())
                .observacoes(observacoes)
                .build();

        Reserva reservaSalva = reservaRepository.save(reserva);
        listaEsperaService.marcarAtendidas(cliente.getId(), quadra.getId(), inicio, fim);
        notificacaoService.enviarConfirmacaoReserva(reservaSalva);
        return reservaSalva;
    }

    /** Quadra ativa e sem bloqueio no periodo. */
    @Transactional(readOnly = true)
    public void validarHorarioReservavel(Quadra quadra, LocalDateTime inicio, LocalDateTime fim) {
        if (quadra.getStatus() != StatusQuadra.DISPONIVEL) {
            throw new IllegalArgumentException("A quadra " + quadra.getNome() + " nao esta disponivel para reservas.");
        }
        var bloqueios = bloqueioQuadraRepository.findConflitantes(quadra.getId(), inicio, fim);
        if (!bloqueios.isEmpty()) {
            String motivo = bloqueios.get(0).getMotivo();
            throw new ConflitoHorarioException("Quadra bloqueada nesse horario"
                    + (motivo != null && !motivo.isBlank() ? " (" + motivo + ")." : "."));
        }
    }

    @Transactional(readOnly = true)
    public PrecoResponse simularPreco(Long quadraId, LocalDateTime inicio, LocalDateTime fim, Long clienteId) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + quadraId));
        return precificacaoService.calcular(quadra, inicio, fim, clienteId);
    }

    @Transactional
    public Reserva cancelar(Long reservaId) {
        return cancelar(reservaId, false);
    }

    /** Cancela aplicando a politica de multa (a menos que isentarMulta seja true). */
    @Transactional
    public Reserva cancelar(Long reservaId, boolean isentarMulta) {
        Reserva reserva = buscarPorId(reservaId);
        exigirConfirmada(reserva, "cancelada");

        BigDecimal taxa = isentarMulta ? BigDecimal.ZERO : simularCancelamento(reserva).taxa();
        auditoria.detalhe("Reserva #" + reservaId + " - multa R$ " + taxa + (isentarMulta ? " (isentada pela equipe)" : ""));
        aplicarEncerramento(reserva, StatusReserva.CANCELADA, taxa);

        Reserva reservaCancelada = reservaRepository.save(reserva);
        notificacaoService.enviarCancelamentoReserva(reservaCancelada);
        listaEsperaService.notificarVagaLiberada(reservaCancelada);
        return reservaCancelada;
    }

    /** Marca nao comparecimento (so apos o inicio) e cobra a multa de no-show configurada. */
    @Transactional
    public Reserva marcarNaoCompareceu(Long reservaId, boolean isentarMulta) {
        Reserva reserva = buscarPorId(reservaId);
        exigirConfirmada(reserva, "marcada como nao comparecimento");
        if (reserva.getInicio().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("So e possivel registrar nao comparecimento depois do horario de inicio.");
        }

        BigDecimal taxa = BigDecimal.ZERO;
        if (!isentarMulta && reserva.getValorTotal() != null) {
            ConfiguracaoReserva cfg = configuracaoService.obter();
            taxa = percentual(reserva.getValorTotal(), cfg.getPercentualMultaNoShow());
        }
        auditoria.detalhe("Reserva #" + reservaId + " - multa de no-show R$ " + taxa + (isentarMulta ? " (isentada pela equipe)" : ""));
        aplicarEncerramento(reserva, StatusReserva.NAO_COMPARECEU, taxa);
        return reservaRepository.save(reserva);
    }

    /** O que aconteceria se a reserva fosse cancelada agora. */
    @Transactional(readOnly = true)
    public PoliticaCancelamentoResponse simularCancelamento(Long reservaId) {
        return simularCancelamento(buscarPorId(reservaId));
    }

    public PoliticaCancelamentoResponse simularCancelamento(Reserva reserva) {
        ConfiguracaoReserva cfg = configuracaoService.obter();
        LocalDateTime gratisAte = reserva.getInicio().minusHours(cfg.getHorasCancelamentoGratis());
        boolean isenta = reserva.getReservaRecorrenteId() != null
                || reserva.getOrigem() == OrigemReserva.AULA
                || reserva.getValorTotal() == null
                || reserva.getValorTotal().signum() <= 0;
        boolean gratis = isenta || !LocalDateTime.now().isAfter(gratisAte);

        BigDecimal taxa = gratis ? BigDecimal.ZERO : percentual(reserva.getValorTotal(), cfg.getPercentualMulta());
        String mensagem;
        if (gratis) {
            mensagem = isenta
                    ? "Cancelamento sem multa."
                    : "Cancelamento gratuito ate " + gratisAte.toLocalDate() + " " + gratisAte.toLocalTime() + ".";
        } else {
            mensagem = "Cancelamento com menos de " + cfg.getHorasCancelamentoGratis()
                    + "h de antecedencia: multa de " + cfg.getPercentualMulta().stripTrailingZeros().toPlainString() + "%.";
        }
        return new PoliticaCancelamentoResponse(gratis, taxa, cfg.getHorasCancelamentoGratis(),
                cfg.getPercentualMulta(), gratisAte, mensagem);
    }

    private void aplicarEncerramento(Reserva reserva, StatusReserva status, BigDecimal taxa) {
        reserva.setStatus(status);
        reserva.setCanceladaEm(LocalDateTime.now());
        if (taxa != null && taxa.signum() > 0) {
            // A multa passa a ser o valor devido da reserva (aparece como pendente no Financeiro).
            reserva.setTaxaCancelamento(taxa);
            reserva.setValorTotal(taxa);
        } else {
            reserva.setTaxaCancelamento(null);
        }
    }

    private void exigirConfirmada(Reserva reserva, String acao) {
        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new IllegalArgumentException("A reserva esta " + reserva.getStatus()
                    + " e nao pode ser " + acao + ".");
        }
    }

    private BigDecimal percentual(BigDecimal valor, BigDecimal pct) {
        if (valor == null || pct == null) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorQuadraEPeriodo(Long quadraId, LocalDateTime inicio, LocalDateTime fim) {
        return reservaRepository.findByQuadraIdAndInicioBetween(quadraId, inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    /**
     * Garante que nao existam reservas ativas conflitantes de horario para a quadra informada.
     * excluirReservaId permite ignorar a propria reserva em uma edicao.
     */
    public void validarDisponibilidade(Long quadraId, LocalDateTime inicio, LocalDateTime fim, Long excluirReservaId) {
        List<Reserva> conflitantes = reservaRepository.findConflitantes(quadraId, inicio, fim);
        boolean haConflito = conflitantes.stream()
                .anyMatch(r -> excluirReservaId == null || !r.getId().equals(excluirReservaId));

        if (haConflito) {
            throw new ConflitoHorarioException(
                    "Ja existe uma reserva para esta quadra no horario solicitado.");
        }
    }
}
