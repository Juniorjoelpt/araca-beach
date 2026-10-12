package com.aracabeach.controller;

import com.aracabeach.dto.MatriculaResponse;
import com.aracabeach.dto.TurmaRequest;
import com.aracabeach.dto.TurmaResponse;
import com.aracabeach.service.MatriculaService;
import com.aracabeach.service.TurmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final TurmaService turmaService;
    private final MatriculaService matriculaService;

    @GetMapping
    public List<TurmaResponse> listar() {
        return turmaService.listarAtivas();
    }

    @GetMapping("/agenda")
    public List<com.aracabeach.dto.TurmaAgendaResponse> agenda(
            @RequestParam Long quadraId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate de,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate ate) {
        return turmaService.agenda(quadraId, de, ate);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaResponse criar(@Valid @RequestBody TurmaRequest request) {
        return turmaService.criar(request);
    }

    @PutMapping("/{id}")
    public TurmaResponse atualizar(@PathVariable Long id, @Valid @RequestBody TurmaRequest request) {
        return turmaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        turmaService.excluir(id);
    }

    @PatchMapping("/{id}/desativar")
    public void desativar(@PathVariable Long id) {
        turmaService.desativar(id);
    }

    @GetMapping("/{id}/matriculas")
    public List<MatriculaResponse> listarMatriculas(@PathVariable Long id) {
        return matriculaService.listarPorTurma(id);
    }
}
