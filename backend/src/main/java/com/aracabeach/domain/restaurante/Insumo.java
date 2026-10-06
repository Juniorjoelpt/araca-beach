package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Ingrediente/produto de estoque do restaurante (carne, queijo, bebida...). Quantidades na unidade do insumo. */
@Entity
@Table(name = "rest_insumos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnidadeInsumo unidade;

    @Column(name = "estoque_atual", nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal estoqueAtual = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false, precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    /** Custo por unidade (R$/kg, R$/L ou R$/un). */
    @Column(name = "custo_unitario", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
