package com.aracabeach.controller;

import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.service.CardapioService;
import com.aracabeach.service.ComandaRestauranteService;
import com.aracabeach.service.InsumoService;
import com.aracabeach.service.RestauranteRelatorioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Cadastros, estoque de insumos, descontos e relatorios do restaurante: somente ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/restaurante/gestao")
@RequiredArgsConstructor
public class RestauranteGestaoController {

    private final CardapioService cardapioService;
    private final InsumoService insumoService;
    private final ComandaRestauranteService comandaService;
    private final RestauranteRelatorioService relatorioService;

    // ---------- categorias ----------

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return cardapioService.listarCategorias();
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criarCategoria(@Valid @RequestBody CategoriaRequest request) {
        return cardapioService.criarCategoria(request);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaResponse atualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return cardapioService.atualizarCategoria(id, request);
    }

    // ---------- itens ----------

    @GetMapping("/itens")
    public List<ItemGestaoResponse> itens() {
        return cardapioService.listarItens();
    }

    @PostMapping("/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemGestaoResponse criarItem(@Valid @RequestBody ItemCardapioRequest request) {
        return cardapioService.criarItem(request);
    }

    @PutMapping("/itens/{id}")
    public ItemGestaoResponse atualizarItem(@PathVariable Long id, @Valid @RequestBody ItemCardapioRequest request) {
        return cardapioService.atualizarItem(id, request);
    }

    @PatchMapping("/itens/{id}/pausa")
    public ItemGestaoResponse pausar(@PathVariable Long id, @Valid @RequestBody PausaRequest request) {
        return cardapioService.pausar(id, request.pausado());
    }

    @PutMapping("/itens/{id}/ficha")
    public ItemGestaoResponse ficha(@PathVariable Long id, @Valid @RequestBody List<@Valid FichaLinhaRequest> linhas) {
        return cardapioService.definirFicha(id, linhas);
    }

    // ---------- insumos ----------

    @GetMapping("/insumos")
    public List<InsumoResponse> insumos() {
        return insumoService.listar();
    }

    @PostMapping("/insumos")
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoResponse criarInsumo(@Valid @RequestBody InsumoRequest request) {
        return insumoService.criar(request);
    }

    @PutMapping("/insumos/{id}")
    public InsumoResponse atualizarInsumo(@PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return insumoService.atualizar(id, request);
    }

    @GetMapping("/insumos/{id}/movimentos")
    public List<MovimentoInsumoResponse> movimentos(@PathVariable Long id) {
        return insumoService.movimentos(id);
    }

    @PostMapping("/insumos/{id}/movimentos")
    public InsumoResponse movimentar(@PathVariable Long id, @Valid @RequestBody MovimentoInsumoRequest request) {
        return insumoService.movimentar(id, request);
    }

    // ---------- desconto e relatorio ----------

    @PatchMapping("/comandas/{id}/desconto")
    public ComandaResponse desconto(@PathVariable Long id, @Valid @RequestBody DescontoRequest request) {
        return comandaService.aplicarDesconto(id, request);
    }

    @GetMapping("/relatorio")
    public RelatorioRestauranteResponse relatorio(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.gerar(inicio, fim);
    }
}
