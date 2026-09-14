package com.aracabeach.relatorio;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private static final DateTimeFormatter FORMATO_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RelatorioPdfService relatorioPdfService;
    private final RelatorioExcelService relatorioExcelService;

    @GetMapping("/fechamento-caixa")
    public ResponseEntity<byte[]> fechamentoCaixa(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) throws Exception {
        byte[] pdf = relatorioPdfService.gerarFechamentoCaixa(data);
        String nomeArquivo = "fechamento-caixa-" + data.format(FORMATO_ARQUIVO) + ".pdf";
        return respostaArquivo(pdf, nomeArquivo, MediaType.APPLICATION_PDF);
    }

    @GetMapping("/comissoes")
    public ResponseEntity<byte[]> comissoes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) throws Exception {
        byte[] pdf = relatorioPdfService.gerarComissoesProfessores(inicio, fim);
        String nomeArquivo = "comissoes-" + inicio.format(FORMATO_ARQUIVO) + "-a-" + fim.format(FORMATO_ARQUIVO) + ".pdf";
        return respostaArquivo(pdf, nomeArquivo, MediaType.APPLICATION_PDF);
    }

    @GetMapping("/reservas")
    public ResponseEntity<byte[]> reservas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) throws Exception {
        byte[] excel = relatorioExcelService.gerarReservas(inicio, fim);
        String nomeArquivo = "reservas-" + inicio.format(FORMATO_ARQUIVO) + "-a-" + fim.format(FORMATO_ARQUIVO) + ".xlsx";
        return respostaArquivo(excel, nomeArquivo, MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/clientes")
    public ResponseEntity<byte[]> clientes() throws Exception {
        byte[] excel = relatorioExcelService.gerarClientes();
        String nomeArquivo = "clientes.xlsx";
        return respostaArquivo(excel, nomeArquivo, MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private ResponseEntity<byte[]> respostaArquivo(byte[] conteudo, String nomeArquivo, MediaType tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(nomeArquivo).build());
        return ResponseEntity.ok().headers(headers).contentType(tipo).body(conteudo);
    }
}
