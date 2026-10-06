package com.aracabeach.controller;

import com.aracabeach.domain.comissao.StatusComissao;
import com.aracabeach.dto.ComissaoLancamentoResponse;
import com.aracabeach.dto.ComissaoResumoProfessorResponse;
import com.aracabeach.dto.PagarComissoesRequest;
import com.aracabeach.service.ComissaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Comissoes automaticas dos professores (somente ADMIN - ver SecurityConfig). */
@RestController
@RequestMapping("/api/comissoes")
@RequiredArgsConstructor
public class ComissaoController {

    private final ComissaoService comissaoService;

    @GetMapping
    public List<ComissaoLancamentoResponse> listar(
            @RequestParam(required = false) Long professorId,
            @RequestParam(required = false) StatusComissao status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return comissaoService.listar(professorId, status, inicio, fim);
    }

    @GetMapping("/resumo")
    public List<ComissaoResumoProfessorResponse> resumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return comissaoService.resumo(inicio, fim);
    }

    @PostMapping("/pagar")
    public List<ComissaoLancamentoResponse> pagar(@Valid @RequestBody PagarComissoesRequest request) {
        return comissaoService.pagar(request);
    }
}
