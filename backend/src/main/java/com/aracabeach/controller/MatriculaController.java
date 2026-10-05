package com.aracabeach.controller;

import com.aracabeach.dto.MatriculaRequest;
import com.aracabeach.dto.MatriculaResponse;
import com.aracabeach.dto.PagamentoMatriculaResponse;
import com.aracabeach.dto.RegistrarPagamentoMatriculaRequest;
import com.aracabeach.service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;

    @GetMapping
    public List<MatriculaResponse> listar() {
        return matriculaService.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse criar(@Valid @RequestBody MatriculaRequest request) {
        return matriculaService.criar(request);
    }

    @PatchMapping("/{id}/cancelar")
    public void cancelar(@PathVariable Long id) {
        matriculaService.cancelar(id);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<MatriculaResponse> listarPorCliente(@PathVariable Long clienteId) {
        return matriculaService.listarPorCliente(clienteId);
    }

    @GetMapping("/{id}/pagamentos")
    public List<PagamentoMatriculaResponse> listarPagamentos(@PathVariable Long id) {
        return matriculaService.listarPagamentos(id);
    }

    @PatchMapping("/pagamentos/{pagamentoId}/pagar")
    public PagamentoMatriculaResponse registrarPagamento(
            @PathVariable Long pagamentoId,
            @Valid @RequestBody RegistrarPagamentoMatriculaRequest request) {
        return matriculaService.registrarPagamento(pagamentoId, request);
    }

    @PostMapping("/gerar-cobrancas")
    public int gerarCobrancasDoMes() {
        return matriculaService.gerarCobrancasDoMes();
    }
}
