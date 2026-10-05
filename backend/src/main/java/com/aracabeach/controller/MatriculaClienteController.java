package com.aracabeach.controller;

import com.aracabeach.dto.MatriculaClienteRequest;
import com.aracabeach.dto.MatriculaClienteResponse;
import com.aracabeach.dto.PagamentoMatriculaClienteResponse;
import com.aracabeach.dto.RegistrarPagamentoMatriculaClienteRequest;
import com.aracabeach.service.MatriculaClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Matricula de cliente com mensalidade unica cobrindo varios horarios de
 * quadra - vive sob /api/mensalidades (mesma regra de acesso: ADMIN) por
 * ser, junto com MensalidadeController, a cobranca mensal da pagina de
 * Mensalidades no frontend.
 */
@RestController
@RequestMapping("/api/mensalidades/matriculas")
@RequiredArgsConstructor
public class MatriculaClienteController {

    private final MatriculaClienteService matriculaClienteService;

    @GetMapping
    public List<MatriculaClienteResponse> listar() {
        return matriculaClienteService.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaClienteResponse criar(@Valid @RequestBody MatriculaClienteRequest request) {
        return matriculaClienteService.criar(request);
    }

    @PatchMapping("/{id}/desativar")
    public void desativar(@PathVariable Long id) {
        matriculaClienteService.desativarMatricula(id);
    }

    @PatchMapping("/horarios/{horarioId}/desativar")
    public void desativarHorario(@PathVariable Long horarioId) {
        matriculaClienteService.desativarHorario(horarioId);
    }

    @GetMapping("/{id}/pagamentos")
    public List<PagamentoMatriculaClienteResponse> listarPagamentos(@PathVariable Long id) {
        return matriculaClienteService.listarPagamentos(id);
    }

    @PatchMapping("/pagamentos/{pagamentoId}/pagar")
    public PagamentoMatriculaClienteResponse registrarPagamento(
            @PathVariable Long pagamentoId,
            @Valid @RequestBody RegistrarPagamentoMatriculaClienteRequest request) {
        return matriculaClienteService.registrarPagamento(pagamentoId, request);
    }

    @PostMapping("/gerar-cobrancas")
    public int gerarCobrancasDoMes() {
        return matriculaClienteService.gerarCobrancasDoMes();
    }
}
