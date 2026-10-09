package com.aracabeach.controller;

import com.aracabeach.config.OperadorAtual;
import com.aracabeach.dto.CaixaHistoricoResponse;
import com.aracabeach.relatorio.CaixaHistoricoPdfService;
import com.aracabeach.service.CaixaHistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Historico de caixa. Operador (RECEPCAO) so ve o proprio; ADMIN ve tudo e filtra por operador. */
@RestController
@RequestMapping("/api/caixa")
@RequiredArgsConstructor
public class CaixaController {

    private final CaixaHistoricoService service;
    private final CaixaHistoricoPdfService pdfService;

    @GetMapping("/historico")
    public CaixaHistoricoResponse historico(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String operador,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String forma) {
        return service.consultar(inicio, fim, operador, origem, forma);
    }

    @GetMapping("/historico/pdf")
    public ResponseEntity<byte[]> historicoPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String operador,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String forma) throws Exception {
        CaixaHistoricoResponse h = service.consultar(inicio, fim, operador, origem, forma);
        String nomeOperador = null;
        if (h.operadorFiltro() != null) {
            nomeOperador = service.operadores().stream()
                    .filter(o -> o.get("login").equalsIgnoreCase(h.operadorFiltro()))
                    .map(o -> o.get("nome")).findFirst().orElse(h.operadorFiltro());
        }
        byte[] pdf = pdfService.gerar(h, nomeOperador);
        String arquivo = "historico-caixa-" + inicio + (inicio.equals(fim) ? "" : "-a-" + fim) + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(arquivo).build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    /** Lista de operadores para o filtro (so ADMIN; o operador comum recebe apenas ele mesmo). */
    @GetMapping("/operadores")
    public List<Map<String, String>> operadores() {
        List<Map<String, String>> todos = service.operadores();
        if (OperadorAtual.admin()) return todos;
        String login = OperadorAtual.login();
        return todos.stream().filter(o -> o.get("login").equalsIgnoreCase(login)).toList();
    }
}
