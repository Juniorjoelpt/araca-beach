package com.aracabeach.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lucro das vendas (restaurante + loja) no periodo: receita - desconto - custo gravado na venda. */
public record LucroResponse(LocalDate inicio, LocalDate fim, Restaurante restaurante, Loja loja,
                            BigDecimal receitaTotal, BigDecimal custoTotal, BigDecimal lucroTotal) {

    /** Receita = itens vendidos; lucro ja abate descontos, custo e a perda das cortesias. */
    public record Restaurante(int comandas, BigDecimal receita, BigDecimal descontos, BigDecimal custo,
                              BigDecimal lucroBruto, int cortesias, BigDecimal perdaCortesias, BigDecimal lucro,
                              List<String> itensSemCusto) {}

    public record Loja(int comandas, BigDecimal receita, BigDecimal custo, BigDecimal lucro, List<String> itensSemCusto) {}
}
