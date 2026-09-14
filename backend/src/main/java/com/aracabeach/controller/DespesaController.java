package com.aracabeach.controller;

import com.aracabeach.domain.despesa.Despesa;
import com.aracabeach.dto.DespesaRequest;
import com.aracabeach.dto.ResumoDespesasResponse;
import com.aracabeach.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
@RequiredArgsConstructor
public class DespesaController {

    private final DespesaService despesaService;

    @GetMapping
    public List<Despesa> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return despesaService.listarPorPeriodo(inicio, fim);
    }

    @GetMapping("/resumo")
    public ResumoDespesasResponse resumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return despesaService.resumo(inicio, fim);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Despesa criar(@Valid @RequestBody DespesaRequest request) {
        return despesaService.criar(request);
    }

    @PatchMapping("/{id}/pagar")
    public Despesa marcarComoPaga(@PathVariable Long id) {
        return despesaService.marcarComoPaga(id);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        despesaService.remover(id);
    }
}
