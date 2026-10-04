package com.aracabeach.controller;

import com.aracabeach.dto.MensalidadeRequest;
import com.aracabeach.dto.MensalidadeResponse;
import com.aracabeach.dto.PagamentoMensalidadeResponse;
import com.aracabeach.dto.RegistrarPagamentoMensalidadeRequest;
import com.aracabeach.service.MensalidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensalidades")
@RequiredArgsConstructor
public class MensalidadeController {

    private final MensalidadeService mensalidadeService;

    @GetMapping
    public List<MensalidadeResponse> listar() {
        return mensalidadeService.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensalidadeResponse criar(@Valid @RequestBody MensalidadeRequest request) {
        return mensalidadeService.criar(request);
    }

    @PatchMapping("/{id}/desativar")
    public void desativar(@PathVariable Long id) {
        mensalidadeService.desativar(id);
    }

    @GetMapping("/{id}/pagamentos")
    public List<PagamentoMensalidadeResponse> listarPagamentos(@PathVariable Long id) {
        return mensalidadeService.listarPagamentos(id);
    }

    @PatchMapping("/pagamentos/{pagamentoId}/pagar")
    public PagamentoMensalidadeResponse registrarPagamento(
            @PathVariable Long pagamentoId,
            @Valid @RequestBody RegistrarPagamentoMensalidadeRequest request) {
        return mensalidadeService.registrarPagamento(pagamentoId, request);
    }

    @PostMapping("/gerar-cobrancas")
    public int gerarCobrancasDoMes() {
        return mensalidadeService.gerarCobrancasDoMes();
    }
}
