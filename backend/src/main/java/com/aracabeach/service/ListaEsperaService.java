package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.regra.ListaEspera;
import com.aracabeach.domain.regra.StatusListaEspera;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.dto.ListaEsperaRequest;
import com.aracabeach.dto.ListaEsperaResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.BloqueioQuadraRepository;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ListaEsperaRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lista de espera de horarios ocupados: quando uma reserva e cancelada, os
 * clientes aguardando aquele horario recebem um e-mail avisando que a vaga
 * abriu (quem reservar primeiro leva). Quando o cliente reserva, o pedido
 * vira ATENDIDA.
 */
@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;
    private final QuadraRepository quadraRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final BloqueioQuadraRepository bloqueioQuadraRepository;
    private final ConfiguracaoReservaService configuracaoService;
    private final NotificacaoService notificacaoService;

    @Transactional
    public ListaEsperaResponse entrar(Long clienteId, ListaEsperaRequest request) {
        if (!configuracaoService.obter().isListaEsperaAtiva()) {
            throw new IllegalArgumentException("A lista de espera nao esta ativa.");
        }
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }
        if (request.inicio().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Nao e possivel entrar na lista de espera de um horario que ja passou.");
        }
        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + clienteId));

        if (reservaRepository.findConflitantes(quadra.getId(), request.inicio(), request.fim()).isEmpty()) {
            throw new IllegalArgumentException("Esse horario esta livre - faca a reserva diretamente.");
        }
        if (!listaEsperaRepository.findAbertosDoClienteSobrepostos(
                clienteId, quadra.getId(), request.inicio(), request.fim()).isEmpty()) {
            throw new IllegalArgumentException("Voce ja esta na lista de espera desse horario.");
        }

        ListaEspera espera = ListaEspera.builder()
                .quadra(quadra)
                .cliente(cliente)
                .inicio(request.inicio())
                .fim(request.fim())
                .build();
        return paraResponse(listaEsperaRepository.save(espera));
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaResponse> listarDoCliente(Long clienteId) {
        return listaEsperaRepository.findByClienteIdOrderByInicioAsc(clienteId).stream()
                .filter(l -> l.getFim().isAfter(LocalDateTime.now()))
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaResponse> listarAbertas() {
        return listaEsperaRepository.findByStatusInAndFimAfterOrderByCriadoEmAsc(
                        List.of(StatusListaEspera.AGUARDANDO, StatusListaEspera.NOTIFICADO), LocalDateTime.now())
                .stream().map(this::paraResponse).toList();
    }

    /** clienteId nulo = cancelamento feito pela equipe (sem checar o dono). */
    @Transactional
    public void cancelar(Long id, Long clienteId) {
        ListaEspera espera = listaEsperaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item da lista de espera nao encontrado: " + id));
        if (clienteId != null && !espera.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Este item nao pertence a voce.");
        }
        espera.setStatus(StatusListaEspera.CANCELADA);
        listaEsperaRepository.save(espera);
    }

    /** Chamado quando uma reserva e cancelada: avisa quem esperava pelo horario, se ele realmente ficou livre. */
    @Transactional
    public void notificarVagaLiberada(Reserva reserva) {
        if (!configuracaoService.obter().isListaEsperaAtiva()) {
            return;
        }
        List<ListaEspera> candidatos = listaEsperaRepository.findAguardandoSobrepostos(
                reserva.getQuadra().getId(), reserva.getInicio(), reserva.getFim());
        for (ListaEspera espera : candidatos) {
            if (!espera.getFim().isAfter(LocalDateTime.now())) {
                continue;
            }
            boolean livre = reservaRepository.findConflitantes(
                            espera.getQuadra().getId(), espera.getInicio(), espera.getFim()).isEmpty()
                    && bloqueioQuadraRepository.findConflitantes(
                            espera.getQuadra().getId(), espera.getInicio(), espera.getFim()).isEmpty();
            if (!livre) {
                continue;
            }
            espera.setStatus(StatusListaEspera.NOTIFICADO);
            espera.setNotificadoEm(LocalDateTime.now());
            listaEsperaRepository.save(espera);
            notificacaoService.enviarVagaLiberada(espera);
        }
    }

    /** Chamado quando o cliente cria uma reserva: encerra pedidos abertos que ela cobre. */
    @Transactional
    public void marcarAtendidas(Long clienteId, Long quadraId, LocalDateTime inicio, LocalDateTime fim) {
        for (ListaEspera espera : listaEsperaRepository.findAbertosDoClienteSobrepostos(clienteId, quadraId, inicio, fim)) {
            espera.setStatus(StatusListaEspera.ATENDIDA);
            listaEsperaRepository.save(espera);
        }
    }

    private ListaEsperaResponse paraResponse(ListaEspera l) {
        return new ListaEsperaResponse(
                l.getId(),
                l.getQuadra().getId(),
                l.getQuadra().getNome(),
                l.getCliente().getId(),
                l.getCliente().getNome(),
                l.getInicio(),
                l.getFim(),
                l.getStatus().name(),
                l.getCriadoEm());
    }
}
