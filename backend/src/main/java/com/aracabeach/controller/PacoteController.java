package com.aracabeach.controller;

import com.aracabeach.domain.pacote.PlanoPacote;
import com.aracabeach.dto.AgendarAulaPacoteRequest;
import com.aracabeach.dto.AulaPacoteResponse;
import com.aracabeach.dto.PacoteClienteResponse;
import com.aracabeach.dto.PagarPacoteRequest;
import com.aracabeach.dto.PlanoPacoteRequest;
import com.aracabeach.dto.PresencaAulaRequest;
import com.aracabeach.dto.VenderPacoteRequest;
import com.aracabeach.service.PacoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Pacotes de aulas (ADMIN e RECEPCAO - ver SecurityConfig). */
@RestController
@RequestMapping("/api/pacotes")
@RequiredArgsConstructor
public class PacoteController {

    private final PacoteService pacoteService;

    @GetMapping("/planos")
    public List<PlanoPacote> planos(@RequestParam(defaultValue = "false") boolean somenteAtivos) {
        return pacoteService.listarPlanos(somenteAtivos);
    }

    @PostMapping("/planos")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoPacote criarPlano(@Valid @RequestBody PlanoPacoteRequest request) {
        return pacoteService.criarPlano(request);
    }

    @PutMapping("/planos/{id}")
    public PlanoPacote atualizarPlano(@PathVariable Long id, @Valid @RequestBody PlanoPacoteRequest request) {
        return pacoteService.atualizarPlano(id, request);
    }

    @GetMapping("/vendas")
    public List<PacoteClienteResponse> vendas(@RequestParam(required = false) Long clienteId) {
        return pacoteService.listar(clienteId);
    }

    @PostMapping("/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    public PacoteClienteResponse vender(@Valid @RequestBody VenderPacoteRequest request) {
        return pacoteService.vender(request);
    }

    @PatchMapping("/vendas/{id}/pagar")
    public PacoteClienteResponse pagar(@PathVariable Long id, @Valid @RequestBody PagarPacoteRequest request) {
        return pacoteService.pagar(id, request.formaPagamento());
    }

    @PatchMapping("/vendas/{id}/cancelar")
    public PacoteClienteResponse cancelar(@PathVariable Long id) {
        return pacoteService.cancelar(id);
    }

    @GetMapping("/vendas/{id}/aulas")
    public List<AulaPacoteResponse> aulasDoPacote(@PathVariable Long id) {
        return pacoteService.listarAulasDoPacote(id);
    }

    @PostMapping("/vendas/{id}/aulas")
    @ResponseStatus(HttpStatus.CREATED)
    public AulaPacoteResponse agendar(@PathVariable Long id, @Valid @RequestBody AgendarAulaPacoteRequest request) {
        return pacoteService.agendarAula(id, request);
    }

    /** Chamada do dia: todas as aulas de pacote da data. */
    @GetMapping("/aulas")
    public List<AulaPacoteResponse> aulasDoDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return pacoteService.listarAulasDoDia(data);
    }

    @PatchMapping("/aulas/{id}/presenca")
    public AulaPacoteResponse presenca(@PathVariable Long id, @Valid @RequestBody PresencaAulaRequest request) {
        return pacoteService.registrarPresenca(id, request.status());
    }
}
