package com.aracabeach.controller;

import com.aracabeach.domain.produto.Comanda;
import com.aracabeach.dto.ComandaRequest;
import com.aracabeach.dto.ItemComandaRequest;
import com.aracabeach.service.ComandaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comandas")
@RequiredArgsConstructor
public class ComandaController {

    private final ComandaService comandaService;

    @GetMapping("/abertas")
    public List<Comanda> listarAbertas() {
        return comandaService.listarAbertas();
    }

    @GetMapping("/{id}")
    public Comanda buscar(@PathVariable Long id) {
        return comandaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Comanda abrir(@Valid @RequestBody ComandaRequest request) {
        return comandaService.abrir(request);
    }

    @PostMapping("/{id}/itens")
    public Comanda adicionarItem(@PathVariable Long id, @Valid @RequestBody ItemComandaRequest request) {
        return comandaService.adicionarItem(id, request);
    }

    @PatchMapping("/{id}/itens/{itemId}/cortesia")
    public Comanda cortesiaItem(@PathVariable Long id, @PathVariable Long itemId,
                                @RequestParam boolean cortesia) {
        return comandaService.definirCortesiaItem(id, itemId, cortesia);
    }

    @PatchMapping("/{id}/desconto")
    public Comanda desconto(@PathVariable Long id, @RequestBody java.util.Map<String, Object> corpo) {
        Object v = corpo.get("valor");
        java.math.BigDecimal valor = v == null || v.toString().isBlank() ? null : new java.math.BigDecimal(v.toString());
        Object m = corpo.get("motivo");
        return comandaService.aplicarDesconto(id, valor, m == null ? null : m.toString());
    }

    @PatchMapping("/{id}/fechar")
    public Comanda fechar(@PathVariable Long id,
                          @RequestParam(required = false) com.aracabeach.domain.financeiro.FormaPagamento forma) {
        return comandaService.fechar(id, forma);
    }
}
