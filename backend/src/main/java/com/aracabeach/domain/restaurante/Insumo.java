package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** VARCHAR (e nao ENUM nativo do MySQL) para novas unidades poderem ser acrescentadas sem alterar a coluna. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private UnidadeInsumo unidade;

    @Column(name = "estoque_atual", nullable = false, precision = 14, scale = 6)
    @Builder.Default
    private BigDecimal estoqueAtual = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false, precision = 14, scale = 6)
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    /** Custo por unidade (R$/kg, R$/L ou R$/un). */
    @Column(name = "custo_unitario", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    /**
     * So para FARDO/PACOTE: quantas unidades (latas, garrafas...) vem em 1 embalagem. Com isso, a ficha tecnica e a
     * venda falam em unidades (1 lata) e o sistema converte: baixa 1/N da embalagem e custa custo/N. Nulo ou 1 = sem conversao.
     */
    @Column(name = "unidades_por_embalagem", precision = 10, scale = 3)
    private BigDecimal unidadesPorEmbalagem;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    /** Divisor aplicado a uma quantidade de uso (ficha/venda) para chegar na unidade de estoque. */
    public BigDecimal fatorUso() {
        boolean embalagem = unidade == UnidadeInsumo.FARDO || unidade == UnidadeInsumo.PACOTE;
        return embalagem && unidadesPorEmbalagem != null && unidadesPorEmbalagem.compareTo(BigDecimal.ONE) > 0
                ? unidadesPorEmbalagem : BigDecimal.ONE;
    }

    /** Custo de 1 unidade de uso (ex.: 1 lata), a partir do custo da embalagem. */
    public BigDecimal custoDeUso() {
        return custoUnitario.divide(fatorUso(), 6, java.math.RoundingMode.HALF_UP);
    }
}
