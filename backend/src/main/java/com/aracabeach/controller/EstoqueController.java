package com.aracabeach.controller;

import com.aracabeach.dto.AjusteEstoqueRequest;
import com.aracabeach.dto.MovimentacaoEstoqueRequest;
import com.aracabeach.dto.MovimentacaoEstoqueResponse;
import com.aracabeach.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @PostMapping("/entrada")
    public MovimentacaoEstoqueResponse registrarEntrada(@Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return estoqueService.registrarEntrada(request);
    }

    @PostMapping("/saida")
    public MovimentacaoEstoqueResponse registrarSaida(@Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return estoqueService.registrarSaida(request);
    }

    @PostMapping("/ajuste")
    public MovimentacaoEstoqueResponse registrarAjuste(@Valid @RequestBody AjusteEstoqueRequest request) {
        return estoqueService.registrarAjuste(request);
    }

    @GetMapping("/movimentacoes")
    public List<MovimentacaoEstoqueResponse> listarMovimentacoes(
            @RequestParam(required = false) Long produtoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return estoqueService.listarMovimentacoes(produtoId, inicio, fim);
    }
}
