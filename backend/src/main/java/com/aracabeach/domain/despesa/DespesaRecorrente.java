package com.aracabeach.domain.despesa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Regra de despesa recorrente (ex.: aluguel todo dia 5). Um job agendado
 * gera automaticamente a Despesa do mes assim que o mes vira, marcando
 * ultimoMesGerado para nao duplicar o lancamento.
 */
@Entity
@Table(name = "despesas_recorrentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaRecorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaDespesa categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;

    /** Formato "yyyy-MM" do ultimo mes em que a despesa foi gerada automaticamente. */
    @Column(name = "ultimo_mes_gerado", length = 7)
    private String ultimoMesGerado;
}
