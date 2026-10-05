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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaResponse criar(@Valid @RequestBody TurmaRequest request) {
        return turmaService.criar(request);
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
