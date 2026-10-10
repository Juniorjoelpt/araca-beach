package com.aracabeach.controller;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.service.CardapioService;
import com.aracabeach.service.ClienteService;
import com.aracabeach.service.ComandaRestauranteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Area do garcom (celular): ver cardapio, abrir comanda (mesa/nome/cliente) e lancar pedidos.
 * Nao permite pagar, descontar, cancelar, fechar nem abrir cortesia - isso e do caixa.
 */
@RestController
@RequestMapping("/api/garcom")
@RequiredArgsConstructor
public class GarcomController {

    private final CardapioService cardapioService;
    private final ComandaRestauranteService comandaService;
    private final ClienteService clienteService;

    public record GarcomAbrirRequest(@Size(max = 40) String mesa, @Size(max = 80) String nome, Long clienteId) {}

    public record ClienteResumo(Long id, String nome, String telefone) {}

    @GetMapping("/cardapio")
    public List<CardapioCategoriaResponse> cardapio() {
        return cardapioService.cardapioAtivo();
    }

    @GetMapping("/comandas")
    public List<ComandaResponse> abertas() {
        return comandaService.listarAbertas();
    }

    @GetMapping("/comandas/{id}")
    public ComandaResponse comanda(@PathVariable Long id) {
        return comandaService.obter(id);
    }

    @PostMapping("/comandas")
    @ResponseStatus(HttpStatus.CREATED)
    public ComandaResponse abrir(@Valid @RequestBody GarcomAbrirRequest r) {
        boolean comCliente = r.clienteId() != null;
        if (!comCliente && (r.mesa() == null || r.mesa().isBlank()) && (r.nome() == null || r.nome().isBlank())) {
            throw new IllegalArgumentException("Informe a mesa ou o nome do cliente.");
        }
        return comandaService.abrir(new ComandaAbrirRequest(null, r.clienteId(), r.mesa(),
                comCliente ? null : Boolean.TRUE, r.nome(), null, null));
    }

    @PostMapping("/comandas/{id}/pedidos")
    public ComandaResponse lancarPedido(@PathVariable Long id, @Valid @RequestBody PedidoRequest request) {
        return comandaService.lancarPedido(id, request);
    }

    @GetMapping("/clientes")
    public List<ClienteResumo> clientes(@RequestParam String busca) {
        String q = norm(busca);
        if (q.length() < 2) return List.of();
        return clienteService.listar().stream()
                .filter(c -> norm(c.getNome()).contains(q) || norm(c.getTelefone()).contains(q))
                .limit(20)
                .map((Cliente c) -> new ClienteResumo(c.getId(), c.getNome(), c.getTelefone()))
                .toList();
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
