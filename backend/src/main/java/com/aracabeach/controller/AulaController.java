package com.aracabeach.controller;

import com.aracabeach.domain.aula.Aula;
import com.aracabeach.dto.AulaRequest;
import com.aracabeach.dto.ComissaoProfessorResponse;
import com.aracabeach.service.AulaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService aulaService;

    @GetMapping
    public List<Aula> listarPorDia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return aulaService.listarPorDia(data);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Aula criar(@Valid @RequestBody AulaRequest request) {
        return aulaService.criar(request);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        aulaService.remover(id);
    }

    @GetMapping("/comissao")
    public ComissaoProfessorResponse comissao(
            @RequestParam Long professorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return aulaService.calcularComissao(professorId, inicio, fim);
    }
}
