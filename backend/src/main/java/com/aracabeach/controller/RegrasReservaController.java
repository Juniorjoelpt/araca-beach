package com.aracabeach.controller;

import com.aracabeach.domain.regra.ConfiguracaoReserva;
import com.aracabeach.dto.BloqueioQuadraRequest;
import com.aracabeach.dto.BloqueioQuadraResponse;
import com.aracabeach.dto.ConfiguracaoReservaRequest;
import com.aracabeach.dto.PrecoResponse;
import com.aracabeach.dto.RegraPrecoRequest;
import com.aracabeach.dto.RegraPrecoResponse;
import com.aracabeach.service.ConfiguracaoReservaService;
import com.aracabeach.service.RegrasReservaService;
import com.aracabeach.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de reserva: leitura liberada para ADMIN e RECEPCAO (a Agenda usa
 * para simular preco), escrita so ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/regras")
@RequiredArgsConstructor
public class RegrasReservaController {

    private final ConfiguracaoReservaService configuracaoService;
    private final RegrasReservaService regrasService;
    private final ReservaService reservaService;

    @GetMapping("/configuracao")
    public ConfiguracaoReserva configuracao() {
        return configuracaoService.obter();
    }

    @PutMapping("/configuracao")
    public ConfiguracaoReserva atualizarConfiguracao(@Valid @RequestBody ConfiguracaoReservaRequest request) {
        return configuracaoService.atualizar(request);
    }

    @GetMapping("/bloqueios")
    public List<BloqueioQuadraResponse> bloqueios() {
        return regrasService.listarBloqueios();
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioQuadraResponse criarBloqueio(@Valid @RequestBody BloqueioQuadraRequest request) {
        return regrasService.criarBloqueio(request);
    }

    @DeleteMapping("/bloqueios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerBloqueio(@PathVariable Long id) {
        regrasService.removerBloqueio(id);
    }

    @GetMapping("/precos")
    public List<RegraPrecoResponse> precos() {
        return regrasService.listarPrecos();
    }

    @PostMapping("/precos")
    @ResponseStatus(HttpStatus.CREATED)
    public RegraPrecoResponse criarPreco(@Valid @RequestBody RegraPrecoRequest request) {
        return regrasService.criarPreco(request);
    }

    @PutMapping("/precos/{id}")
    public RegraPrecoResponse atualizarPreco(@PathVariable Long id, @Valid @RequestBody RegraPrecoRequest request) {
        return regrasService.atualizarPreco(id, request);
    }

    @DeleteMapping("/precos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerPreco(@PathVariable Long id) {
        regrasService.removerPreco(id);
    }

    /** Simula o preco de uma reserva (considera pico/fora de pico e desconto de mensalista). */
    @GetMapping("/preco")
    public PrecoResponse preco(
            @RequestParam Long quadraId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return reservaService.simularPreco(quadraId, inicio, fim, clienteId);
    }
}
