package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.ReservaFinanceiroResponse;
import com.aracabeach.exception.ConflitoHorarioException;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import com.aracabeach.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negocio do portal do cliente (app mobile). Reaproveita as
 * mesmas entidades/repositorios do sistema interno - o portal e so mais
 * uma "porta de entrada" para criar Reserva, com a diferenca de que o
 * clienteId vem sempre do token autenticado (nunca do corpo da requisicao,
 * para um cliente jamais conseguir reservar em nome de outro).
 */
@Service
@RequiredArgsConstructor
public class PortalReservaService {

    private final QuadraRepository quadraRepository;
    private final ReservaRepository reservaRepository;
    private final PagamentoService pagamentoService;

    @Value("${araca-beach.portal.horario-abertura:6}")
    private int horarioAbertura;

    @Value("${araca-beach.portal.horario-fechamento:23}")
    private int horarioFechamento;

    @Value("${araca-beach.portal.duracao-slot-minutos:60}")
    private int duracaoSlotMinutos;

    @Transactional(readOnly = true)
    public List<PortalQuadraResponse> listarQuadrasDisponiveis() {
        return quadraRepository.findAll().stream()
                .map(q -> new PortalQuadraResponse(q.getId(), q.getNome(), q.getTipo().name(), q.getValorHora(), q.getCapacidade()))
                .toList();
    }

    /**
     * Gera os horarios do dia (a cada duracaoSlotMinutos, entre a abertura
     * e o fechamento configurados) e marca quais ja estao ocupados por uma
     * reserva existente naquela quadra.
     */
    @Transactional(readOnly = true)
    public List<PortalSlotResponse> disponibilidade(Long quadraId, LocalDate data) {
        Quadra quadra = quadraRepository.findById(quadraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra não encontrada: " + quadraId));

        LocalDateTime inicioDia = LocalDateTime.of(data, LocalTime.MIN);
        LocalDateTime fimDia = LocalDateTime.of(data, LocalTime.MAX);
        List<Reserva> reservasDoDia = reservaRepository.findByQuadraIdAndInicioBetween(quadra.getId(), inicioDia, fimDia)
                .stream()
                .filter(r -> r.getStatus() != StatusReserva.CANCELADA)
                .toList();

        List<PortalSlotResponse> slots = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.of(data, LocalTime.of(horarioAbertura, 0));
        LocalDateTime limite = LocalDateTime.of(data, LocalTime.of(horarioFechamento, 0));

        while (cursor.isBefore(limite)) {
            LocalDateTime fimSlot = cursor.plusMinutes(duracaoSlotMinutos);
            LocalDateTime inicioSlot = cursor;

            boolean ocupado = reservasDoDia.stream()
                    .anyMatch(r -> inicioSlot.isBefore(r.getFim()) && fimSlot.isAfter(r.getInicio()));

            slots.add(new PortalSlotResponse(inicioSlot, fimSlot, !ocupado));
            cursor = fimSlot;
        }

        return slots;
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

        boolean conflito = !reservaRepository.findConflitantes(quadra.getId(), request.inicio(), request.fim()).isEmpty();
        if (conflito) {
            throw new ConflitoHorarioException("Esse horário acabou de ser reservado por outra pessoa. Escolha outro horário.");
        }

        BigDecimal horas = BigDecimal.valueOf(Duration.between(request.inicio(), request.fim()).toMinutes())
                .divide(BigDecimal.valueOf(60));
        BigDecimal valorTotal = quadra.getValorHora().multiply(horas);

        // NOTA PARA IMPLEMENTACAO FUTURA DE PAGAMENTO ONLINE:
        // por enquanto a reserva feita pelo portal ja nasce CONFIRMADA, sem
        // exigir pagamento (igual a uma reserva feita por telefone). Quando
        // o pagamento online for implementado, o fluxo aqui deve mudar para:
        // 1) criar a reserva com um status de "aguardando pagamento",
        // 2) chamar o gateway de pagamento (Pix/cartao) para gerar a cobranca,
        // 3) so confirmar a reserva quando o webhook do gateway avisar que
        //    o pagamento foi aprovado (e cancelar/liberar o horario se o
        //    pagamento expirar sem ser concluido).
        Reserva reserva = Reserva.builder()
                .quadra(quadra)
                .cliente(cliente)
                .inicio(request.inicio())
                .fim(request.fim())
                .status(StatusReserva.CONFIRMADA)
                .origem(OrigemReserva.ONLINE)
                .valorTotal(valorTotal)
                .build();

        reserva = reservaRepository.save(reserva);
        return paraResponse(reserva, "PENDENTE");
    }

    @Transactional(readOnly = true)
    public List<PortalReservaResponse> minhasReservas(Cliente cliente) {
        return reservaRepository.findByClienteIdOrderByInicioDesc(cliente.getId()).stream()
                .map(r -> paraResponse(r, null))
                .toList();
    }

    @Transactional
    public void cancelarReserva(Cliente cliente, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada: " + reservaId));

        if (!reserva.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Esta reserva não pertence a você.");
        }
        if (reserva.getInicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível cancelar uma reserva que já passou.");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reservaRepository.save(reserva);
    }

    private PortalReservaResponse paraResponse(Reserva reserva, String statusPagamentoConhecido) {
        String statusPagamento = statusPagamentoConhecido;
        if (statusPagamento == null) {
            statusPagamento = pagamentoService.listarPorReserva(reserva.getId()).isEmpty() ? "PENDENTE" : "PAGO";
        }
        return new PortalReservaResponse(
                reserva.getId(),
                reserva.getQuadra().getNome(),
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getStatus().name(),
                reserva.getValorTotal(),
                statusPagamento
        );
    }
}
