package com.aracabeach.controller;

import com.aracabeach.domain.despesa.DespesaRecorrente;
import com.aracabeach.dto.DespesaRecorrenteRequest;
import com.aracabeach.service.DespesaRecorrenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/despesas-recorrentes")
@RequiredArgsConstructor
public class DespesaRecorrenteController {

    private final DespesaRecorrenteService despesaRecorrenteService;

    @GetMapping
    public List<DespesaRecorrente> listar() {
        return despesaRecorrenteService.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DespesaRecorrente criar(@Valid @RequestBody DespesaRecorrenteRequest request) {
        return despesaRecorrenteService.criar(request);
    }

    @PatchMapping("/{id}/desativar")
    public void desativar(@PathVariable Long id) {
        despesaRecorrenteService.desativar(id);
    }
}
