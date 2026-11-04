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

    @PutMapping("/{id}")
    public Aula atualizar(@PathVariable Long id, @Valid @RequestBody AulaRequest request) {
        return aulaService.atualizar(id, request);
    }

    @PatchMapping("/{id}/pagamento")
    public Aula pagar(@PathVariable Long id,
                      @RequestBody java.util.Map<String, com.aracabeach.domain.financeiro.FormaPagamento> corpo) {
        return aulaService.registrarPagamento(id, corpo.get("formaPagamento"));
    }

    @PostMapping("/{id}/alunos")
    @ResponseStatus(HttpStatus.CREATED)
    public Aula adicionarAluno(@PathVariable Long id, @Valid @RequestBody com.aracabeach.dto.AulaParticipanteRequest request) {
        return aulaService.adicionarAluno(id, request);
    }

    @PatchMapping("/{id}/alunos/{alunoId}/pagamento")
    public Aula pagarAluno(@PathVariable Long id, @PathVariable Long alunoId,
                           @RequestBody java.util.Map<String, com.aracabeach.domain.financeiro.FormaPagamento> corpo) {
        return aulaService.pagarAluno(id, alunoId, corpo.get("formaPagamento"));
    }

    /** Somente ADMIN (ver SecurityConfig): estorna o pagamento no caixa e apaga a aula. */
    @DeleteMapping("/{id}/estornar")
    public java.util.Map<String, String> estornar(@PathVariable Long id) {
        return java.util.Map.of("mensagem", aulaService.estornarERemover(id));
    }

    @DeleteMapping("/{id}/alunos/{alunoId}")
    public Aula removerAluno(@PathVariable Long id, @PathVariable Long alunoId) {
        return aulaService.removerAluno(id, alunoId);
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
