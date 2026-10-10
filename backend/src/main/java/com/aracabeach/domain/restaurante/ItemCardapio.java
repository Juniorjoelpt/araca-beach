package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rest_itens_cardapio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaCardapio categoria;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    /** Ex.: "Serve 2 pessoas". */
    @Column(length = 80)
    private String porcao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Praca praca = Praca.COZINHA;

    /** Inativo = removido do cardapio (nao aparece para venda). */
    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    /** Pausado = "acabou hoje": continua cadastrado, mas nao pode ser lancado. */
    @Column(nullable = false)
    @Builder.Default
    private boolean pausado = false;

    @Column(name = "tempo_preparo_min")
    private Integer tempoPreparoMin;

    @Column(nullable = false)
    @Builder.Default
    private int ordem = 0;

    /** Codigo de barras (EAN/UPC) de produtos industrializados, lido pelo leitor no caixa. Unico por item. */
    @Column(name = "codigo_barras", length = 30, unique = true)
    private String codigoBarras;

    /** Custo adicional de producao por unidade (mao de obra, gas, embalagem...), somado ao custo dos insumos da ficha. */
    @Column(name = "custo_producao", precision = 10, scale = 2)
    private java.math.BigDecimal custoProducao;
}
