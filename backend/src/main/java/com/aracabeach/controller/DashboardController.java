package com.aracabeach.controller;

import com.aracabeach.dto.DashboardResponse;
import com.aracabeach.dto.EstatisticaDiaResponse;
import com.aracabeach.dto.OcupacaoQuadraResponse;
import com.aracabeach.dto.GerencialResponse;
import com.aracabeach.service.DashboardService;
import com.aracabeach.service.GerencialService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final GerencialService gerencialService;

    @GetMapping
    public DashboardResponse obter(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return dashboardService.gerar(data != null ? data : LocalDate.now());
    }

    @GetMapping("/faturamento-periodo")
    public List<EstatisticaDiaResponse> faturamentoPeriodo(@RequestParam(defaultValue = "7") int dias) {
        return dashboardService.faturamentoUltimosDias(dias);
    }

    @GetMapping("/ocupacao-quadras")
    public List<OcupacaoQuadraResponse> ocupacaoQuadras(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboardService.ocupacaoPorQuadra(inicio, fim);
    }

    /** Indicadores gerenciais do periodo (somente ADMIN - ver SecurityConfig). */
    @GetMapping("/gerencial")
    public GerencialResponse gerencial(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return gerencialService.gerar(inicio, fim);
    }
}
