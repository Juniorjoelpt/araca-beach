package com.aracabeach.domain.produto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaProduto categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    @Builder.Default
    private Integer estoque = 0;

    /** Custo de compra por unidade (para o calculo de lucro). Nulo = nao informado. */
    @Column(precision = 10, scale = 2)
    private BigDecimal custo;

    /** Para equipamentos (ex.: aluguel de raquete), indica se e item de aluguel e nao de venda. */
    @Column(name = "eh_aluguel")
    @Builder.Default
    private boolean ehAluguel = false;
}
