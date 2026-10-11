package com.aracabeach.controller;

import com.aracabeach.dto.DayUseDtos.*;
import com.aracabeach.service.DayUseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Day Use. Operacao (venda, entrada, cancelamento): ADMIN e RECEPCAO. Tipos e capacidade: so ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/dayuse")
@RequiredArgsConstructor
public class DayUseController {

    private final DayUseService service;

    @GetMapping("/tipos")
    public List<TipoResponse> tipos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.tiposAtivos(data != null ? data : LocalDate.now());
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        return service.config();
    }

    @GetMapping("/dia")
    public DiaResponse dia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.dia(data != null ? data : LocalDate.now());
    }

    @PostMapping("/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse vender(@Valid @RequestBody VendaRequest request) {
        return service.vender(request);
    }

    @PatchMapping("/vendas/{id}/entrada")
    public VendaResponse entrada(@PathVariable Long id, @Valid @RequestBody EntradaRequest request) {
        return service.registrarEntrada(id, request.quantidade());
    }

    @PatchMapping("/vendas/{id}/pagamento")
    public VendaResponse confirmarPagamento(@PathVariable Long id, @Valid @RequestBody ConfirmarPagamentoRequest request) {
        return service.confirmarPagamento(id, request.forma());
    }

    @PatchMapping("/vendas/{id}/cancelar")
    public VendaResponse cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        return service.cancelar(id, motivo);
    }

    // ---------- gestao (ADMIN) ----------

    @GetMapping("/gestao/tipos")
    public List<TipoResponse> todosOsTipos() {
        return service.todosOsTipos();
    }

    @PostMapping("/gestao/tipos")
    @ResponseStatus(HttpStatus.CREATED)
    public TipoResponse criarTipo(@Valid @RequestBody TipoRequest request) {
        return service.criarTipo(request);
    }

    @PutMapping("/gestao/tipos/{id}")
    public TipoResponse atualizarTipo(@PathVariable Long id, @Valid @RequestBody TipoRequest request) {
        return service.atualizarTipo(id, request);
    }

    @PutMapping("/gestao/config")
    public ConfigResponse salvarConfig(@Valid @RequestBody ConfigRequest request) {
        return service.salvarConfig(request);
    }
}
