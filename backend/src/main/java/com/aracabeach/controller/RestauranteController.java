package com.aracabeach.controller;

import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.service.CardapioService;
import com.aracabeach.service.ComandaRestauranteService;
import com.aracabeach.service.ReservaMesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Operacao diaria do restaurante (caixa): ADMIN e RECEPCAO. */
@RestController
@RequestMapping("/api/restaurante")
@RequiredArgsConstructor
public class RestauranteController {

    private final CardapioService cardapioService;
    private final ComandaRestauranteService comandaService;
    private final ReservaMesaService reservaMesaService;

    @GetMapping("/cardapio")
    public List<CardapioCategoriaResponse> cardapio() {
        return cardapioService.cardapioAtivo();
    }

    // ---------- comandas ----------

    @GetMapping("/comandas/abertas")
    public List<ComandaResponse> abertas() {
        return comandaService.listarAbertas();
    }

    @GetMapping("/comandas/{id}")
    public ComandaResponse comanda(@PathVariable Long id) {
        return comandaService.obter(id);
    }

    @PostMapping("/comandas")
    @ResponseStatus(HttpStatus.CREATED)
    public ComandaResponse abrir(@Valid @RequestBody ComandaAbrirRequest request) {
        return comandaService.abrir(request);
    }

    @PostMapping("/comandas/{id}/pedidos")
    public ComandaResponse lancarPedido(@PathVariable Long id, @Valid @RequestBody PedidoRequest request) {
        return comandaService.lancarPedido(id, request);
    }

    @DeleteMapping("/comandas/{id}/itens/{itemId}")
    public ComandaResponse cancelarItem(@PathVariable Long id, @PathVariable Long itemId,
                                        @RequestParam(required = false) String motivo) {
        return comandaService.cancelarItem(id, itemId, motivo);
    }

    @PatchMapping("/comandas/{id}/mesa")
    public ComandaResponse transferirMesa(@PathVariable Long id, @Valid @RequestBody MesaRequest request) {
        return comandaService.transferirMesa(id, request.mesa());
    }

    @PatchMapping("/comandas/{id}/taxa-servico")
    public ComandaResponse taxaServico(@PathVariable Long id, @Valid @RequestBody TaxaServicoRequest request) {
        return comandaService.definirTaxaServico(id, request.percentual());
    }

    @PostMapping("/comandas/{id}/pagamentos")
    public ComandaResponse pagar(@PathVariable Long id, @Valid @RequestBody PagamentoRestRequest request) {
        return comandaService.registrarPagamento(id, request);
    }

    @DeleteMapping("/comandas/{id}/pagamentos/{pagamentoId}")
    public ComandaResponse removerPagamento(@PathVariable Long id, @PathVariable Long pagamentoId) {
        return comandaService.removerPagamento(id, pagamentoId);
    }

    @PatchMapping("/comandas/{id}/fechar")
    public ComandaResponse fechar(@PathVariable Long id) {
        return comandaService.fechar(id);
    }

    @PatchMapping("/comandas/{id}/cancelar")
    public ComandaResponse cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        return comandaService.cancelar(id, motivo);
    }

    // ---------- reservas de mesa ----------

    @GetMapping("/reservas-mesa")
    public List<ReservaMesaResponse> reservasMesa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return reservaMesaService.listarDoDia(data != null ? data : LocalDate.now());
    }

    @PostMapping("/reservas-mesa")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaMesaResponse criarReservaMesa(@Valid @RequestBody ReservaMesaRequest request) {
        return reservaMesaService.criar(request);
    }

    @PatchMapping("/reservas-mesa/{id}/mesa")
    public ReservaMesaResponse mesaDaReserva(@PathVariable Long id, @Valid @RequestBody MesaRequest request) {
        return reservaMesaService.definirMesa(id, request.mesa());
    }

    @PatchMapping("/reservas-mesa/{id}/cancelar")
    public ReservaMesaResponse cancelarReservaMesa(@PathVariable Long id) {
        return reservaMesaService.cancelar(id);
    }

    @PatchMapping("/reservas-mesa/{id}/nao-compareceu")
    public ReservaMesaResponse naoCompareceu(@PathVariable Long id) {
        return reservaMesaService.naoCompareceu(id);
    }
}
