package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.produto.Comanda;
import com.aracabeach.domain.produto.ItemComanda;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.dto.ClienteHistoricoResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ComandaRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ClienteHistoricoService {

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final ComandaRepository comandaRepository;

    @Transactional(readOnly = true)
    public ClienteHistoricoResponse obter(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId));

        var reservas = reservaRepository.findByClienteIdOrderByInicioDesc(clienteId).stream()
                .map(this::paraReservaItem)
                .toList();

        var comandas = comandaRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(this::paraComandaItem)
                .toList();

        BigDecimal totalReservas = reservas.stream()
                .filter(r -> !"CANCELADA".equals(r.status()))
                .map(ClienteHistoricoResponse.ReservaHistoricoItem::valorTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLoja = comandas.stream()
                .map(ClienteHistoricoResponse.ComandaHistoricoItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClienteHistoricoResponse(cliente.getId(), cliente.getNome(), reservas, comandas, totalReservas, totalLoja);
    }

    private ClienteHistoricoResponse.ReservaHistoricoItem paraReservaItem(Reserva r) {
        return new ClienteHistoricoResponse.ReservaHistoricoItem(
                r.getId(), r.getQuadra().getNome(), r.getInicio(), r.getFim(), r.getStatus().name(), r.getValorTotal()
        );
    }

    private ClienteHistoricoResponse.ComandaHistoricoItem paraComandaItem(Comanda c) {
        BigDecimal total = c.getItens().stream()
                .map(ItemComanda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ClienteHistoricoResponse.ComandaHistoricoItem(c.getId(), c.getCriadoEm(), c.isFechada(), total);
    }
}
