package com.aracabeach.controller;

import com.aracabeach.domain.torneio.Inscricao;
import com.aracabeach.domain.torneio.Torneio;
import com.aracabeach.dto.ConfrontoResponse;
import com.aracabeach.dto.InscricaoRequest;
import com.aracabeach.dto.RegistrarResultadoRequest;
import com.aracabeach.dto.TorneioRequest;
import com.aracabeach.service.ChaveamentoService;
import com.aracabeach.service.TorneioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/torneios")
@RequiredArgsConstructor
public class TorneioController {

    private final TorneioService torneioService;
    private final ChaveamentoService chaveamentoService;

    @GetMapping
    public List<Torneio> listar() {
        return torneioService.listarTodos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Torneio criar(@Valid @RequestBody TorneioRequest request) {
        return torneioService.criar(request);
    }

    @GetMapping("/{id}/inscricoes")
    public List<Inscricao> listarInscricoes(@PathVariable Long id) {
        return torneioService.listarInscricoes(id);
    }

    @PostMapping("/inscricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Inscricao inscrever(@Valid @RequestBody InscricaoRequest request) {
        return torneioService.inscrever(request);
    }

    @PostMapping("/{id}/chaveamento")
    public List<ConfrontoResponse> gerarChaveamento(@PathVariable Long id, @RequestParam String categoria) {
        return chaveamentoService.gerar(id, categoria);
    }

    @GetMapping("/{id}/chaveamento")
    public List<ConfrontoResponse> listarChaveamento(@PathVariable Long id, @RequestParam String categoria) {
        return chaveamentoService.listar(id, categoria);
    }

    @PatchMapping("/confrontos/{confrontoId}/resultado")
    public List<ConfrontoResponse> registrarResultado(
            @PathVariable Long confrontoId, @Valid @RequestBody RegistrarResultadoRequest request) {
        return chaveamentoService.registrarResultado(confrontoId, request);
    }
}
