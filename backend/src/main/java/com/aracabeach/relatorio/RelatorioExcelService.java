package com.aracabeach.relatorio;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera planilhas .xlsx (Apache POI) para exportacao de dados do sistema.
 */
@Service
@RequiredArgsConstructor
public class RelatorioExcelService {

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;

    public byte[] gerarReservas(LocalDate inicio, LocalDate fim) throws IOException {
        List<Reserva> reservas = reservaRepository.findByInicioBetween(
                LocalDateTime.of(inicio, LocalTime.MIN), LocalDateTime.of(fim, LocalTime.MAX));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet planilha = workbook.createSheet("Reservas");
            CellStyle estiloCabecalho = criarEstiloCabecalho(workbook);

            String[] colunas = {"Data/Hora início", "Data/Hora fim", "Quadra", "Cliente", "Status", "Origem", "Valor (R$)"};
            Row linhaCabecalho = planilha.createRow(0);
            for (int i = 0; i < colunas.length; i++) {
                Cell celula = linhaCabecalho.createCell(i);
                celula.setCellValue(colunas[i]);
                celula.setCellStyle(estiloCabecalho);
            }

            int linhaIndice = 1;
            for (Reserva r : reservas) {
                Row linha = planilha.createRow(linhaIndice++);
                linha.createCell(0).setCellValue(r.getInicio().format(FORMATO_DATA_HORA));
                linha.createCell(1).setCellValue(r.getFim().format(FORMATO_DATA_HORA));
                linha.createCell(2).setCellValue(r.getQuadra().getNome());
                linha.createCell(3).setCellValue(r.getCliente().getNome());
                linha.createCell(4).setCellValue(r.getStatus().name());
                linha.createCell(5).setCellValue(r.getOrigem().name());
                linha.createCell(6).setCellValue(r.getValorTotal() != null ? r.getValorTotal().doubleValue() : 0);
            }

            for (int i = 0; i < colunas.length; i++) {
                planilha.autoSizeColumn(i);
            }

            return paraBytes(workbook);
        }
    }

    public byte[] gerarClientes() throws IOException {
        List<Cliente> clientes = clienteRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet planilha = workbook.createSheet("Clientes");
            CellStyle estiloCabecalho = criarEstiloCabecalho(workbook);

            String[] colunas = {"Nome", "Telefone", "E-mail", "CPF"};
            Row linhaCabecalho = planilha.createRow(0);
            for (int i = 0; i < colunas.length; i++) {
                Cell celula = linhaCabecalho.createCell(i);
                celula.setCellValue(colunas[i]);
                celula.setCellStyle(estiloCabecalho);
            }

            int linhaIndice = 1;
            for (Cliente c : clientes) {
                Row linha = planilha.createRow(linhaIndice++);
                linha.createCell(0).setCellValue(c.getNome());
                linha.createCell(1).setCellValue(c.getTelefone() != null ? c.getTelefone() : "");
                linha.createCell(2).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                linha.createCell(3).setCellValue(c.getCpf() != null ? c.getCpf() : "");
            }

            for (int i = 0; i < colunas.length; i++) {
                planilha.autoSizeColumn(i);
            }

            return paraBytes(workbook);
        }
    }

    private CellStyle criarEstiloCabecalho(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fonte = workbook.createFont();
        fonte.setBold(true);
        fonte.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fonte);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private byte[] paraBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        workbook.write(saida);
        return saida.toByteArray();
    }
}
