package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.ReservaRequest;
import com.aracabeach.exception.ConflitoHorarioException;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negocio do agendamento de quadras.
 * A validacao central é impedir reservas sobrepostas na mesma quadra.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final QuadraRepository quadraRepository;
    private final ClienteRepository clienteRepository;
    private final NotificacaoService notificacaoService;

    @Transactional
    public Reserva criar(ReservaRequest request) {
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }

        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + request.clienteId()));

        validarDisponibilidade(quadra.getId(), request.inicio(), request.fim(), null);

        BigDecimal horas = BigDecimal.valueOf(Duration.between(request.inicio(), request.fim()).toMinutes())
                .divide(BigDecimal.valueOf(60));
        BigDecimal valorTotal = quadra.getValorHora().multiply(horas);

        Reserva reserva = Reserva.builder()
                .quadra(quadra)
                .cliente(cliente)
                .inicio(request.inicio())
                .fim(request.fim())
                .status(StatusReserva.CONFIRMADA)
                .valorTotal(valorTotal)
                .observacoes(request.observacoes())
                .build();

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.enviarConfirmacaoReserva(reservaSalva);
        return reservaSalva;
    }

    @Transactional
    public Reserva cancelar(Long reservaId) {
        Reserva reserva = buscarPorId(reservaId);
        reserva.setStatus(StatusReserva.CANCELADA);
        Reserva reservaCancelada = reservaRepository.save(reserva);
        notificacaoService.enviarCancelamentoReserva(reservaCancelada);
        return reservaCancelada;
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
    private void validarDisponibilidade(Long quadraId, LocalDateTime inicio, LocalDateTime fim, Long excluirReservaId) {
        List<Reserva> conflitantes = reservaRepository.findConflitantes(quadraId, inicio, fim);
        boolean haConflito = conflitantes.stream()
                .anyMatch(r -> excluirReservaId == null || !r.getId().equals(excluirReservaId));

        if (haConflito) {
            throw new ConflitoHorarioException(
                    "Ja existe uma reserva para esta quadra no horario solicitado.");
        }
    }
}
