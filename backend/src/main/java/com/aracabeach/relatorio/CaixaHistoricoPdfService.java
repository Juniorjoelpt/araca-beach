package com.aracabeach.relatorio;

import com.aracabeach.config.OperadorAtual;
import com.aracabeach.dto.CaixaHistoricoResponse;
import com.aracabeach.dto.CaixaHistoricoResponse.Lancamento;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** PDF do historico de caixa, na identidade visual da Araca Beach (azul marinho + verde limao, logo). */
@Service
@RequiredArgsConstructor
public class CaixaHistoricoPdfService {

    private static final Color AZUL = new Color(0x06, 0x12, 0x68);
    private static final Color VERDE = new Color(0xB7, 0xE9, 0x0C);
    private static final Color VERDE_ESCURO = new Color(0x2C, 0x69, 0x06);
    private static final Color AREIA = new Color(0xFB, 0xF6, 0xEC);
    private static final Color CINZA = new Color(0x6B, 0x72, 0x80);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] gerar(CaixaHistoricoResponse h, String operadorNomeFiltro) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 32, 32, 28, 44);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, saida);
        writer.setPageEvent(new Rodape());
        doc.open();

        String periodo = h.inicio().equals(h.fim())
                ? h.inicio().format(DATA)
                : h.inicio().format(DATA) + " a " + h.fim().format(DATA);
        doc.add(cabecalho("Histórico de Caixa", "Período: " + periodo
                + (operadorNomeFiltro != null ? "   |   Operador: " + operadorNomeFiltro : "   |   Todos os operadores")));

        doc.add(resumo(h));

        Paragraph titulo = new Paragraph("Recebimentos detalhados", fonte(13, Font.BOLD, AZUL));
        titulo.setSpacingBefore(14);
        titulo.setSpacingAfter(6);
        doc.add(titulo);

        if (h.lancamentos().isEmpty()) {
            doc.add(new Paragraph("Nenhum recebimento no período e filtros selecionados.", fonte(10, Font.NORMAL, CINZA)));
        } else {
            doc.add(tabelaDetalhe(h));
        }

        doc.close();
        return saida.toByteArray();
    }

    // ---------- blocos ----------

    private PdfPTable cabecalho(String titulo, String subtitulo) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 6f});
        t.setWidthPercentage(100);

        PdfPCell logo = new PdfPCell();
        logo.setBackgroundColor(AZUL);
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setPadding(8);
        try (InputStream in = new ClassPathResource("relatorio/logo-araca.png").getInputStream()) {
            Image img = Image.getInstance(in.readAllBytes());
            img.scaleToFit(70, 58);
            logo.addElement(img);
        } catch (Exception e) {
            logo.addElement(new Paragraph("ARAÇA BEACH", fonte(12, Font.BOLD, VERDE)));
        }
        t.addCell(logo);

        PdfPCell texto = new PdfPCell();
        texto.setBackgroundColor(AZUL);
        texto.setBorder(Rectangle.NO_BORDER);
        texto.setPadding(10);
        texto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph p1 = new Paragraph(titulo, fonte(22, Font.BOLD, Color.WHITE));
        Paragraph p2 = new Paragraph(subtitulo, fonte(10, Font.NORMAL, VERDE));
        p2.setSpacingBefore(3);
        texto.addElement(p1);
        texto.addElement(p2);
        t.addCell(texto);

        PdfPCell faixa = new PdfPCell(new Phrase(" ", fonte(3, Font.NORMAL, VERDE)));
        faixa.setColspan(2);
        faixa.setBackgroundColor(VERDE);
        faixa.setBorder(Rectangle.NO_BORDER);
        faixa.setFixedHeight(5);
        t.addCell(faixa);
        t.setSpacingAfter(12);
        return t;
    }

    private PdfPTable resumo(CaixaHistoricoResponse h) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 1.6f, 1.6f, 1.6f});
        t.setWidthPercentage(100);

        // total geral
        PdfPCell total = new PdfPCell();
        total.setBackgroundColor(VERDE);
        total.setBorder(Rectangle.NO_BORDER);
        total.setPadding(10);
        total.addElement(new Paragraph("TOTAL RECEBIDO", fonte(8, Font.BOLD, AZUL)));
        Paragraph valor = new Paragraph(moeda(h.total()), fonte(20, Font.BOLD, AZUL));
        valor.setSpacingBefore(4);
        total.addElement(valor);
        total.addElement(new Paragraph(h.quantidade() + " recebimento(s)", fonte(8, Font.NORMAL, AZUL)));
        t.addCell(total);

        t.addCell(blocoTotais("Por forma de pagamento", h.porForma(), true));
        t.addCell(blocoTotais("Por origem", h.porOrigem(), false));
        t.addCell(blocoTotais("Por operador", h.porOperador(), false));
        return t;
    }

    private PdfPCell blocoTotais(String titulo, Map<String, BigDecimal> dados, boolean forma) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(AREIA);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(8);
        c.addElement(new Paragraph(titulo.toUpperCase(), fonte(8, Font.BOLD, VERDE_ESCURO)));
        if (dados.isEmpty()) {
            c.addElement(new Paragraph("—", fonte(9, Font.NORMAL, CINZA)));
        }
        PdfPTable linhas = new PdfPTable(new float[]{3f, 2f});
        linhas.setWidthPercentage(100);
        linhas.setSpacingBefore(4);
        dados.forEach((k, v) -> {
            String rotulo = forma ? rotuloForma(k) : (titulo.contains("origem") ? rotuloOrigem(k) : k);
            linhas.addCell(semBorda(rotulo, fonte(9, Font.NORMAL, AZUL), Element.ALIGN_LEFT));
            linhas.addCell(semBorda(moeda(v), fonte(9, Font.BOLD, AZUL), Element.ALIGN_RIGHT));
        });
        c.addElement(linhas);
        return c;
    }

    private PdfPTable tabelaDetalhe(CaixaHistoricoResponse h) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.35f, 1.3f, 4.2f, 1.4f, 1.6f, 1.2f});
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (String col : new String[]{"Data/hora", "Origem", "Descrição", "Forma", "Operador", "Valor"}) {
            PdfPCell c = new PdfPCell(new Phrase(col, fonte(9, Font.BOLD, VERDE)));
            c.setBackgroundColor(AZUL);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(6);
            if (col.equals("Valor")) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(c);
        }
        boolean zebra = false;
        for (Lancamento l : h.lancamentos()) {
            Color fundo = zebra ? AREIA : Color.WHITE;
            t.addCell(celula(l.dataHora().format(DATA_HORA), fundo, Element.ALIGN_LEFT, false));
            t.addCell(celula(rotuloOrigem(l.origem()), fundo, Element.ALIGN_LEFT, false));
            t.addCell(celula(l.descricao(), fundo, Element.ALIGN_LEFT, false));
            t.addCell(celula(rotuloForma(l.forma()), fundo, Element.ALIGN_LEFT, false));
            t.addCell(celula(l.operadorNome() != null ? l.operadorNome() : "—", fundo, Element.ALIGN_LEFT, false));
            t.addCell(celula(moeda(l.valor()), fundo, Element.ALIGN_RIGHT, true));
            zebra = !zebra;
        }
        PdfPCell rotulo = new PdfPCell(new Phrase("TOTAL", fonte(10, Font.BOLD, AZUL)));
        rotulo.setColspan(5);
        rotulo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rotulo.setBackgroundColor(VERDE);
        rotulo.setBorder(Rectangle.NO_BORDER);
        rotulo.setPadding(6);
        t.addCell(rotulo);
        PdfPCell soma = new PdfPCell(new Phrase(moeda(h.total()), fonte(10, Font.BOLD, AZUL)));
        soma.setHorizontalAlignment(Element.ALIGN_RIGHT);
        soma.setBackgroundColor(VERDE);
        soma.setBorder(Rectangle.NO_BORDER);
        soma.setPadding(6);
        t.addCell(soma);
        return t;
    }

    // ---------- utilidades ----------

    private PdfPCell celula(String texto, Color fundo, int alinhamento, boolean negrito) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, fonte(8.5f, negrito ? Font.BOLD : Font.NORMAL, AZUL)));
        c.setBackgroundColor(fundo);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new Color(0xE5, 0xE0, 0xD2));
        c.setPadding(5);
        c.setHorizontalAlignment(alinhamento);
        return c;
    }

    private PdfPCell semBorda(String texto, Font f, int alinhamento) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(2);
        c.setHorizontalAlignment(alinhamento);
        return c;
    }

    private static Font fonte(float tamanho, int estilo, Color cor) {
        return new Font(Font.HELVETICA, tamanho, estilo, cor);
    }

    static String moeda(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
        String[] partes = x.toPlainString().split("\\.");
        String inteiro = partes[0].replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
        return "R$ " + inteiro + "," + partes[1];
    }

    static String rotuloForma(String f) {
        return switch (f) {
            case "PIX" -> "Pix";
            case "CARTAO_CREDITO" -> "Cartão de crédito";
            case "CARTAO_DEBITO" -> "Cartão de débito";
            case "DINHEIRO" -> "Dinheiro";
            default -> f;
        };
    }

    static String rotuloOrigem(String o) {
        return switch (o) {
            case "RESERVA" -> "Quadras";
            case "RESTAURANTE" -> "Restaurante";
            case "LOJA" -> "Loja";
            case "MENSALIDADE" -> "Mensalidade";
            case "MATRICULA" -> "Aulas";
            case "PACOTE" -> "Pacote de aulas";
            default -> "Outros";
        };
    }

    /** Rodape em todas as paginas: dados da arena, quem emitiu e numero da pagina. */
    private static class Rodape extends PdfPageEventHelper {
        private final String emitidoPor = OperadorAtual.login();
        private final String emissao = LocalDateTime.now().format(DATA_HORA);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 14;
            cb.setColorStroke(VERDE);
            cb.setLineWidth(1.5f);
            cb.moveTo(document.left(), y + 10);
            cb.lineTo(document.right(), y + 10);
            cb.stroke();
            Font f = fonte(8, Font.NORMAL, CINZA);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Araça Beach · Araçagy · São José de Ribamar – MA · @aracabeach", f), document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Emitido em " + emissao + (emitidoPor != null ? " por " + emitidoPor : ""), f),
                    (document.left() + document.right()) / 2 + 60, y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), f), document.right(), y, 0);
        }
    }
}
