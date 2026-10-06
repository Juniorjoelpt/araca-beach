package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.quadra.StatusQuadra;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.PoliticaCancelamentoResponse;
import com.aracabeach.dto.PrecoResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.BloqueioQuadraRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import com.aracabeach.service.PagamentoService;
import com.aracabeach.service.PrecificacaoService;
import com.aracabeach.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negocio do portal do cliente (app mobile). Usa o mesmo nucleo do
 * painel interno (ReservaService) - preco, bloqueio, conflito, multa de
 * cancelamento -, com a diferenca de que o clienteId vem sempre do token
 * autenticado (nunca do corpo da requisicao).
 */
@Service
@RequiredArgsConstructor
public class PortalReservaService {

    private final QuadraRepository quadraRepository;
    private final ReservaRepository reservaRepository;
    private final PagamentoService pagamentoService;
    private final ReservaService reservaService;
    private final PrecificacaoService precificacaoService;
    private final BloqueioQuadraRepository bloqueioQuadraRepository;

    @Value("${araca-beach.portal.horario-abertura:6}")
    private int horarioAbertura;

    @Value("${araca-beach.portal.horario-fechamento:23}")
    private int horarioFechamento;

    @Value("${araca-beach.portal.duracao-slot-minutos:60}")
    private int duracaoSlotMinutos;

    @Transactional(readOnly = true)
    public List<PortalQuadraResponse> listarQuadrasDisponiveis() {
        return quadraRepository.findAll().stream()
                .filter(q -> q.getStatus() == StatusQuadra.DISPONIVEL)
                .map(q -> new PortalQuadraResponse(q.getId(), q.getNome(), q.getTipo().name(), q.getValorHora(), q.getCapacidade()))
                .toList();
    }

    /**
     * Gera os horarios do dia (a cada duracaoSlotMinutos, entre a abertura
     * e o fechamento configurados), com o preco de cada um e o motivo de
     * indisponibilidade (ocupado, bloqueado ou ja passou).
     */
    @Transactional(readOnly = true)
    public List<PortalSlotResponse> disponibilidade(Long quadraId, LocalDate data) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada: " + quadraId));

        LocalDateTime inicioDia = LocalDateTime.of(data, LocalTime.MIN);
        LocalDateTime fimDia = LocalDateTime.of(data, LocalTime.MAX);
        List<Reserva> reservasDoDia = reservaRepository.findByQuadraIdAndInicioBetween(quadra.getId(), inicioDia, fimDia)
                .stream()
                .filter(r -> r.getStatus() != StatusReserva.CANCELADA && r.getStatus() != StatusReserva.NAO_COMPARECEU)
                .toList();
        boolean quadraAtiva = quadra.getStatus() == StatusQuadra.DISPONIVEL;
        LocalDateTime agora = LocalDateTime.now();

        List<PortalSlotResponse> slots = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.of(data, LocalTime.of(horarioAbertura, 0));
        LocalDateTime limite = LocalDateTime.of(data, LocalTime.of(horarioFechamento, 0));

        while (cursor.isBefore(limite)) {
            LocalDateTime fimSlot = cursor.plusMinutes(duracaoSlotMinutos);
            LocalDateTime inicioSlot = cursor;

            boolean ocupado = reservasDoDia.stream()
                    .anyMatch(r -> inicioSlot.isBefore(r.getFim()) && fimSlot.isAfter(r.getInicio()));
            boolean bloqueado = !quadraAtiva
                    || !bloqueioQuadraRepository.findConflitantes(quadra.getId(), inicioSlot, fimSlot).isEmpty();
            boolean passado = inicioSlot.isBefore(agora);

            String motivo = null;
            if (passado) {
                motivo = "PASSADO";
            } else if (bloqueado) {
                motivo = "BLOQUEADO";
            } else if (ocupado) {
                motivo = "OCUPADO";
            }

            BigDecimal preco = precificacaoService.calcular(quadra, inicioSlot, fimSlot, null).valorTotal();
            slots.add(new PortalSlotResponse(inicioSlot, fimSlot, motivo == null, preco, motivo));
            cursor = fimSlot;
        }

        return slots;
    }

    /** Preco da reserva para o cliente logado (com desconto de mensalista, se houver). */
    @Transactional(readOnly = true)
    public PrecoResponse preco(Cliente cliente, Long quadraId, LocalDateTime inicio, LocalDateTime fim) {
        return reservaService.simularPreco(quadraId, inicio, fim, cliente.getId());
    }

    @Transactional
    public PortalReservaResponse criarReserva(Cliente cliente, PortalReservaRequest request) {
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O horário de término deve ser posterior ao de início.");
        }
        if (request.inicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível reservar um horário no passado.");
        }

        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada: " + request.quadraId()));

        // NOTA PARA IMPLEMENTACAO FUTURA DE PAGAMENTO ONLINE:
        // por enquanto a reserva feita pelo portal ja nasce CONFIRMADA, sem
        // exigir pagamento (igual a uma reserva feita por telefone). Quando
        // o pagamento online for implementado, o fluxo aqui deve mudar para:
        // 1) criar a reserva com um status de "aguardando pagamento",
        // 2) chamar o gateway de pagamento (Pix/cartao) para gerar a cobranca,
        // 3) so confirmar a reserva quando o webhook do gateway avisar que
        //    o pagamento foi aprovado (e cancelar/liberar o horario se o
        //    pagamento expirar sem ser concluido).
        Reserva reserva = reservaService.criarReserva(quadra, cliente, request.inicio(), request.fim(),
                OrigemReserva.ONLINE, null);
        return paraResponse(reserva, "PENDENTE");
    }

    @Transactional(readOnly = true)
    public List<PortalReservaResponse> minhasReservas(Cliente cliente) {
        return reservaRepository.findByClienteIdOrderByInicioDesc(cliente.getId()).stream()
                .filter(r -> r.getOrigem() != OrigemReserva.AULA) // aulas de pacote aparecem em "Aulas"
                .map(r -> paraResponse(r, null))
                .toList();
    }

    @Transactional
    public PortalReservaResponse cancelarReserva(Cliente cliente, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada: " + reservaId));

        if (!reserva.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Esta reserva não pertence a você.");
        }
        if (reserva.getInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível cancelar uma reserva que já passou.");
        }

        Reserva cancelada = reservaService.cancelar(reservaId, false);
        return paraResponse(cancelada, null);
    }

    /** Politica vigente (para exibir antes de reservar/cancelar). */
    @Transactional(readOnly = true)
    public PoliticaCancelamentoResponse politicaDaReserva(Cliente cliente, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada: " + reservaId));
        if (!reserva.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Esta reserva não pertence a você.");
        }
        return reservaService.simularCancelamento(reserva);
    }

    private PortalReservaResponse paraResponse(Reserva reserva, String statusPagamentoConhecido) {
        String statusPagamento = statusPagamentoConhecido;
        if (statusPagamento == null) {
            BigDecimal pago = pagamentoService.listarPorReserva(reserva.getId()).stream()
                    .map(p -> p.getValor())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = reserva.getValorTotal() != null ? reserva.getValorTotal() : BigDecimal.ZERO;
            if (pago.signum() <= 0) {
                statusPagamento = "PENDENTE";
            } else {
                statusPagamento = pago.compareTo(total) >= 0 ? "PAGO" : "PARCIAL";
            }
        }

        BigDecimal taxaAgora = null;
        LocalDateTime gratisAte = null;
        String mensagem = null;
        if (reserva.getStatus() == StatusReserva.CONFIRMADA && reserva.getInicio().isAfter(LocalDateTime.now())) {
            PoliticaCancelamentoResponse politica = reservaService.simularCancelamento(reserva);
            taxaAgora = politica.taxa();
            gratisAte = politica.gratisAte();
            mensagem = politica.mensagem();
        }

        return new PortalReservaResponse(
                reserva.getId(),
                reserva.getQuadra().getNome(),
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getStatus().name(),
                reserva.getValorTotal(),
                statusPagamento,
                reserva.getTaxaCancelamento(),
                taxaAgora,
                gratisAte,
                mensagem
        );
    }
}
