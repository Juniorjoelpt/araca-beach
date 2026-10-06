package com.aracabeach.domain.pacote;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Modelo de pacote de aulas vendido ao cliente (ex.: "10 aulas de Futevolei"). */
@Entity
@Table(name = "planos_pacote")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoPacote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /** Modalidade (mesmos valores de TipoQuadra) ou vazio para qualquer. */
    @Column(length = 20)
    private String tipo;

    @Column(nullable = false)
    private int quantidadeAulas;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private int validadeDias;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;
}
