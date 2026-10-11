package com.aracabeach.domain.dayuse;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "day_use_itens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayUseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private DayUseVenda venda;

    @Column(name = "tipo_id")
    private Long tipoId;

    @Column(name = "tipo_nome", nullable = false, length = 80)
    private String tipoNome;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    /** Pessoas cobertas por unidade (pacote de grupo > 1). Nulo = 1. */
    @Column(name = "pessoas_por_unidade")
    private Integer pessoasPorUnidade;

    @Column(name = "custo_unitario", precision = 10, scale = 2)
    private BigDecimal custoUnitario;
}
