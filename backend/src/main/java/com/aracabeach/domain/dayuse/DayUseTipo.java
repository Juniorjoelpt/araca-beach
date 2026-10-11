package com.aracabeach.domain.dayuse;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Tipo de ingresso do Day Use (ex.: Adulto, Crianca). Preco de fim de semana e opcional. */
@Entity
@Table(name = "day_use_tipos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayUseTipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "preco_semana", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoSemana;

    /** Preco de sabado e domingo. Nulo = mesmo preco da semana. */
    @Column(name = "preco_fim_semana", precision = 10, scale = 2)
    private BigDecimal precoFimSemana;

    /** Quantas pessoas 1 ingresso deste tipo cobre (1 = individual; 10 = pacote para grupo de 10). Nulo = 1. */
    @Column(name = "pessoas_por_unidade")
    private Integer pessoasPorUnidade;

    /** Custo por ingresso (opcional), so para o calculo de lucro. */
    @Column(precision = 10, scale = 2)
    private BigDecimal custo;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
