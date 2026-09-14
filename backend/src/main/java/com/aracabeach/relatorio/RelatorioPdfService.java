package com.aracabeach.relatorio;

import com.aracabeach.domain.despesa.Despesa;
import com.aracabeach.domain.professor.Professor;
import com.aracabeach.dto.ComissaoProfessorResponse;
import com.aracabeach.dto.ResumoDespesasResponse;
import com.aracabeach.dto.ResumoCaixaResponse;
import com.aracabeach.repository.ProfessorRepository;
import com.aracabeach.service.AulaService;
import com.aracabeach.service.DespesaService;
import com.aracabeach.service.PagamentoService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera relatorios em PDF (fechamento de caixa e comissao de professores)
 * usando a biblioteca OpenPDF. Documentos simples: titulo, periodo,
 * tabelas de dados e totais - pensados para impressao/arquivo, nao para
 * apresentacao visual sofisticada.
 */
@Service
@RequiredArgsConstructor
public class RelatorioPdfService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color AZUL_MARCA = new Color(6, 18, 104);
    private static final Color VERDE_MARCA = new Color(44, 105, 6);

    private final PagamentoService pagamentoService;
    private final DespesaService despesaService;
    private final AulaService aulaService;
    private final ProfessorRepository professorRepository;

    public byte[] gerarFechamentoCaixa(LocalDate data) throws DocumentException, IOException {
        Document documento = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PdfWriter.getInstance(documento, saida);
        documento.open();

        adicionarCabecalho(documento, "Fechamento de Caixa", "Data: " + data.format(FORMATO_DATA));

        ResumoCaixaResponse resumoCaixa = pagamentoService.resumoCaixa(data);
        ResumoDespesasResponse resumoDespesas = despesaService.resumo(data, data);

        // Resumo de entradas
        documento.add(subtitulo("Entradas (pagamentos recebidos)"));
        PdfPTable tabelaEntradas = new PdfPTable(2);
        tabelaEntradas.setWidthPercentage(100);
        tabelaEntradas.setSpacingBefore(8);
        tabelaEntradas.setSpacingAfter(12);
        adicionarLinha(tabelaEntradas, "Total recebido", formatarMoeda(resumoCaixa.totalRecebido()), true);
        resumoCaixa.porFormaPagamento().forEach((forma, valor) ->
                adicionarLinha(tabelaEntradas, "  " + forma, formatarMoeda(valor), false));
        adicionarLinha(tabelaEntradas, "Quantidade de pagamentos", String.valueOf(resumoCaixa.quantidadePagamentos()), false);
        documento.add(tabelaEntradas);

        // Resumo de saidas
        documento.add(subtitulo("Saídas (despesas pagas)"));
        List<Despesa> despesasPagas = despesaService.listarPorPeriodo(data, data).stream()
                .filter(Despesa::isPaga)
                .toList();
        PdfPTable tabelaSaidas = new PdfPTable(2);
        tabelaSaidas.setWidthPercentage(100);
        tabelaSaidas.setSpacingBefore(8);
        tabelaSaidas.setSpacingAfter(12);
        if (despesasPagas.isEmpty()) {
            adicionarLinha(tabelaSaidas, "Nenhuma despesa paga nesse dia", "", false);
        } else {
            despesasPagas.forEach(d -> adicionarLinha(tabelaSaidas, d.getDescricao(), formatarMoeda(d.getValor()), false));
        }
        adicionarLinha(tabelaSaidas, "Total de despesas", formatarMoeda(resumoDespesas.totalPago()), true);
        documento.add(tabelaSaidas);

        // Saldo liquido
        BigDecimal saldo = resumoCaixa.totalRecebido().subtract(resumoDespesas.totalPago());
        Paragraph saldoParagrafo = new Paragraph("Saldo líquido do dia: " + formatarMoeda(saldo),
                new Font(Font.HELVETICA, 13, Font.BOLD, saldo.signum() >= 0 ? VERDE_MARCA : Color.RED));
        saldoParagrafo.setSpacingBefore(10);
        documento.add(saldoParagrafo);

        documento.close();
        return saida.toByteArray();
    }

    public byte[] gerarComissoesProfessores(LocalDate inicio, LocalDate fim) throws DocumentException, IOException {
        Document documento = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PdfWriter.getInstance(documento, saida);
        documento.open();

        adicionarCabecalho(documento, "Relatório de Comissões",
                "Período: " + inicio.format(FORMATO_DATA) + " a " + fim.format(FORMATO_DATA));

        List<Professor> professores = professorRepository.findAll();

        PdfPTable tabela = new PdfPTable(5);
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(10);
        tabela.setWidths(new float[]{2.5f, 1f, 1.5f, 1f, 1.5f});

        adicionarCabecalhoTabela(tabela, "Professor", "Aulas", "Valor total", "% comis.", "Comissão");

        BigDecimal totalGeralComissao = BigDecimal.ZERO;
        for (Professor professor : professores) {
            ComissaoProfessorResponse comissao = aulaService.calcularComissao(professor.getId(), inicio, fim);
            if (comissao.quantidadeAulas() == 0) continue;

            adicionarLinhaTabela(tabela,
                    comissao.professorNome(),
                    String.valueOf(comissao.quantidadeAulas()),
                    formatarMoeda(comissao.valorTotalAulas()),
                    comissao.percentualComissao() + "%",
                    formatarMoeda(comissao.valorComissao()));
            totalGeralComissao = totalGeralComissao.add(comissao.valorComissao());
        }
        documento.add(tabela);

        Paragraph totalParagrafo = new Paragraph("Total geral de comissões: " + formatarMoeda(totalGeralComissao),
                new Font(Font.HELVETICA, 13, Font.BOLD, VERDE_MARCA));
        totalParagrafo.setSpacingBefore(14);
        documento.add(totalParagrafo);

        documento.close();
        return saida.toByteArray();
    }

    private void adicionarCabecalho(Document documento, String titulo, String subtitulo) throws DocumentException {
        Paragraph marca = new Paragraph("ARAÇA BEACH", new Font(Font.HELVETICA, 11, Font.BOLD, AZUL_MARCA));
        documento.add(marca);

        Paragraph tituloParagrafo = new Paragraph(titulo, new Font(Font.HELVETICA, 18, Font.BOLD, AZUL_MARCA));
        tituloParagrafo.setSpacingBefore(4);
        documento.add(tituloParagrafo);

        Paragraph subtituloParagrafo = new Paragraph(subtitulo, new Font(Font.HELVETICA, 11, Font.NORMAL, Color.GRAY));
        subtituloParagrafo.setSpacingAfter(16);
        documento.add(subtituloParagrafo);
    }

    private Paragraph subtitulo(String texto) {
        return new Paragraph(texto, new Font(Font.HELVETICA, 13, Font.BOLD, AZUL_MARCA));
    }

    private void adicionarLinha(PdfPTable tabela, String label, String valor, boolean destaque) {
        Font fonteLabel = new Font(Font.HELVETICA, 10, destaque ? Font.BOLD : Font.NORMAL);
        Font fonteValor = new Font(Font.HELVETICA, 10, destaque ? Font.BOLD : Font.NORMAL);
        PdfPCell celulaLabel = new PdfPCell(new Phrase(label, fonteLabel));
        celulaLabel.setBorder(Rectangle.NO_BORDER);
        celulaLabel.setPaddingBottom(4);
        PdfPCell celulaValor = new PdfPCell(new Phrase(valor, fonteValor));
        celulaValor.setBorder(Rectangle.NO_BORDER);
        celulaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celulaValor.setPaddingBottom(4);
        tabela.addCell(celulaLabel);
        tabela.addCell(celulaValor);
    }

    private void adicionarCabecalhoTabela(PdfPTable tabela, String... colunas) {
        for (String coluna : colunas) {
            PdfPCell celula = new PdfPCell(new Phrase(coluna, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            celula.setBackgroundColor(AZUL_MARCA);
            celula.setPadding(6);
            tabela.addCell(celula);
        }
    }

    private void adicionarLinhaTabela(PdfPTable tabela, String... valores) {
        for (String valor : valores) {
            PdfPCell celula = new PdfPCell(new Phrase(valor, new Font(Font.HELVETICA, 10, Font.NORMAL)));
            celula.setPadding(6);
            tabela.addCell(celula);
        }
    }

    private String formatarMoeda(BigDecimal valor) {
        return "R$ " + (valor != null ? valor.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
    }
}
