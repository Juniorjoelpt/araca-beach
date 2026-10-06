package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.restaurante.ComandaRestaurante;
import com.aracabeach.domain.restaurante.ReservaMesa;
import com.aracabeach.domain.restaurante.StatusComandaRestaurante;
import com.aracabeach.domain.restaurante.StatusReservaMesa;
import com.aracabeach.dto.RestauranteDtos.ReservaMesaRequest;
import com.aracabeach.dto.RestauranteDtos.ReservaMesaResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ComandaRestauranteRepository;
import com.aracabeach.repository.ReservaMesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reservas de mesa do restaurante. O rotulo da mesa e livre (nao ha numero fixo de mesas). */
@Service
@RequiredArgsConstructor
public class ReservaMesaService {

    private final ReservaMesaRepository repository;
    private final ClienteRepository clienteRepository;
    private final ComandaRestauranteRepository comandaRepository;

    @Transactional(readOnly = true)
    public List<ReservaMesaResponse> listarDoDia(LocalDate data) {
        List<ReservaMesa> reservas = repository.findByDataHoraBetweenOrderByDataHora(
                LocalDateTime.of(data, LocalTime.MIN), LocalDateTime.of(data, LocalTime.MAX));
        Map<Long, Long> comandaPorReserva = reservas.isEmpty() ? Map.of()
                : comandaRepository.findByReservaMesaIdIn(reservas.stream().map(ReservaMesa::getId).toList()).stream()
                .filter(c -> c.getStatus() != StatusComandaRestaurante.CANCELADA)
                .collect(Collectors.toMap(c -> c.getReservaMesa().getId(), ComandaRestaurante::getId, (a, b) -> a));
        return reservas.stream().map(r -> paraResponse(r, comandaPorReserva.get(r.getId()))).toList();
    }

    @Transactional
    public ReservaMesaResponse criar(ReservaMesaRequest r) {
        Cliente cliente = clienteRepository.findById(r.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + r.clienteId()));
        if (r.dataHora().isBefore(LocalDateTime.now().minusHours(2))) {
            throw new IllegalArgumentException("A data/hora da reserva já passou.");
        }
        ReservaMesa reserva = ReservaMesa.builder()
                .cliente(cliente)
                .dataHora(r.dataHora())
                .pessoas(r.pessoas())
                .mesa(limpar(r.mesa()))
                .observacoes(limpar(r.observacoes()))
                .build();
        return paraResponse(repository.save(reserva), null);
    }

    @Transactional
    public ReservaMesaResponse definirMesa(Long id, String mesa) {
        ReservaMesa reserva = buscar(id);
        if (reserva.getStatus() == StatusReservaMesa.CANCELADA || reserva.getStatus() == StatusReservaMesa.NAO_COMPARECEU
                || reserva.getStatus() == StatusReservaMesa.CONCLUIDA) {
            throw new IllegalArgumentException("Esta reserva já foi encerrada.");
        }
        if (limpar(mesa) == null && reserva.getStatus() == StatusReservaMesa.EM_ATENDIMENTO) {
            throw new IllegalArgumentException("Uma reserva em atendimento precisa de uma mesa.");
        }
        reserva.setMesa(limpar(mesa));
        repository.save(reserva);
        comandaRepository.findFirstByReservaMesaIdAndStatusNot(id, StatusComandaRestaurante.CANCELADA)
                .filter(c -> c.getStatus() == StatusComandaRestaurante.ABERTA)
                .ifPresent(c -> { c.setMesa(reserva.getMesa()); comandaRepository.save(c); });
        return paraResponse(reserva, idComanda(id));
    }

    @Transactional
    public ReservaMesaResponse cancelar(Long id) {
        ReservaMesa reserva = buscar(id);
        if (reserva.getStatus() != StatusReservaMesa.CONFIRMADA) {
            throw new IllegalArgumentException("Só é possível cancelar reservas confirmadas (sem atendimento iniciado).");
        }
        reserva.setStatus(StatusReservaMesa.CANCELADA);
        return paraResponse(repository.save(reserva), null);
    }

    @Transactional
    public ReservaMesaResponse naoCompareceu(Long id) {
        ReservaMesa reserva = buscar(id);
        if (reserva.getStatus() != StatusReservaMesa.CONFIRMADA) {
            throw new IllegalArgumentException("Só é possível marcar não comparecimento de reservas confirmadas.");
        }
        if (reserva.getDataHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("A reserva ainda não chegou no horário.");
        }
        reserva.setStatus(StatusReservaMesa.NAO_COMPARECEU);
        return paraResponse(repository.save(reserva), null);
    }

    public ReservaMesa buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva de mesa não encontrada: " + id));
    }

    private Long idComanda(Long reservaId) {
        return comandaRepository.findFirstByReservaMesaIdAndStatusNot(reservaId, StatusComandaRestaurante.CANCELADA)
                .map(ComandaRestaurante::getId).orElse(null);
    }

    private ReservaMesaResponse paraResponse(ReservaMesa r, Long comandaId) {
        return new ReservaMesaResponse(r.getId(), r.getCliente().getId(), r.getCliente().getNome(),
                r.getCliente().getTelefone(), r.getDataHora(), r.getPessoas(), r.getMesa(), r.getStatus(),
                r.getObservacoes(), comandaId);
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
