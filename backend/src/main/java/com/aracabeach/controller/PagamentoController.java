package com.aracabeach.controller;

import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.dto.CobrancaPendenteResponse;
import com.aracabeach.dto.PagamentoRequest;
import com.aracabeach.dto.ReservaFinanceiroResponse;
import com.aracabeach.dto.ResumoCaixaResponse;
import com.aracabeach.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pagamento registrar(@Valid @RequestBody PagamentoRequest request) {
        return pagamentoService.registrar(request);
    }

    @GetMapping
    public List<Pagamento> listarPorReserva(@RequestParam Long reservaId) {
        return pagamentoService.listarPorReserva(reservaId);
    }

    @GetMapping("/dia")
    public List<ReservaFinanceiroResponse> visaoDoDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return pagamentoService.visaoFinanceiraDoDia(data);
    }

    @GetMapping("/resumo")
    public ResumoCaixaResponse resumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return pagamentoService.resumoCaixa(data);
    }

    @GetMapping("/cobrancas-pendentes")
    public List<CobrancaPendenteResponse> cobrancasPendentes() {
        return pagamentoService.listarCobrancasPendentes();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        pagamentoService.excluir(id);
    }
}
