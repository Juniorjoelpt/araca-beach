package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rest_itens_pedido")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoRestaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoRestaurante pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemCardapio item;

    /** Copia do nome/preco no momento da venda (o cardapio pode mudar depois). */
    @Column(name = "nome_snapshot", nullable = false, length = 120)
    private String nome;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    @Column(length = 200)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Praca praca;

    @Column(nullable = false)
    @Builder.Default
    private boolean cancelado = false;

    @Column(name = "motivo_cancelamento", length = 200)
    private String motivoCancelamento;

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
