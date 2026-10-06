package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Indicadores gerenciais de um periodo, com comparativo ao periodo anterior de mesmo tamanho. */
public record GerencialResponse(
        LocalDate inicio,
        LocalDate fim,
        LocalDate inicioAnterior,
        LocalDate fimAnterior,
        Indicadores atual,
        Indicadores anterior,
        Map<String, BigDecimal> receitaPorForma,
        Map<String, BigDecimal> despesaPorCategoria,
        List<OcupacaoQuadraHoras> ocupacaoPorQuadra,
        List<Heat> mapaDeCalor,
        List<TopCliente> topClientes,
        Inadimplencia inadimplencia,
        List<ReceitaDia> receitaDiaria
) {
    public record Indicadores(
            BigDecimal receitaCaixa,
            BigDecimal receitaLoja,
            BigDecimal receitaTotal,
            BigDecimal despesasPagas,
            BigDecimal resultado,
            int reservas,
            int canceladas,
            int naoCompareceu,
            BigDecimal taxaCancelamentoPercentual,
            BigDecimal multasCobradas,
            BigDecimal ticketMedioReserva,
            BigDecimal horasReservadas,
            int clientesNovos
    ) {}

    public record OcupacaoQuadraHoras(String quadra, BigDecimal horasReservadas, BigDecimal horasDisponiveis,
                                      BigDecimal ocupacaoPercentual) {}

    /** diaSemana 1=segunda ... 7=domingo; hora 0-23; quantidade de reservas que iniciaram nessa faixa. */
    public record Heat(int diaSemana, int hora, int quantidade) {}

    public record TopCliente(Long clienteId, String nome, int reservas, BigDecimal valor) {}

    public record Inadimplencia(BigDecimal valorEmAtraso, int quantidadeEmAtraso, BigDecimal valorAVencer,
                                int quantidadeAVencer, long maiorAtrasoDias) {}

    public record ReceitaDia(LocalDate data, BigDecimal receita, BigDecimal despesa) {}
}
