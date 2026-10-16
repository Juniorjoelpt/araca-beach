package com.aracabeach.domain.produto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_comanda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemComanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private Comanda comanda;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    /** Custo de 1 unidade no momento da venda (copiado do produto). Nulo = custo desconhecido. */
    @Column(name = "custo_unitario", precision = 10, scale = 2)
    private BigDecimal custoUnitario;

    /** Item dado como cortesia: sai do estoque, mas nao e cobrado nem entra no caixa. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean cortesia = false;

    /** Valor cobrado do item (zero quando e cortesia). */
    @Transient
    public BigDecimal getSubtotal() {
        return cortesia ? BigDecimal.ZERO : getValorReferencia();
    }

    /** Valor de tabela do item, mesmo sendo cortesia. */
    @Transient
    public BigDecimal getValorReferencia() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
