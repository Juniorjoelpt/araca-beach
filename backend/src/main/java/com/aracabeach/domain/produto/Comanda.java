package com.aracabeach.domain.produto;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.reserva.Reserva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comandas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemComanda> itens = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean fechada = false;

    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    /** Preenchidos ao fechar a comanda (venda da loja entra no caixa). */
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private com.aracabeach.domain.financeiro.FormaPagamento formaPagamento;

    @Column(name = "fechada_em")
    private LocalDateTime fechadaEm;

    @Column(length = 120)
    private String operador;

    @Column(name = "desconto_valor", precision = 10, scale = 2)
    private BigDecimal descontoValor;

    @Column(name = "desconto_motivo", length = 200)
    private String descontoMotivo;

    public BigDecimal getDescontoValor() {
        return descontoValor == null ? BigDecimal.ZERO : descontoValor;
    }

    /** Soma dos itens cobrados (cortesias ficam de fora), antes do desconto. */
    @Transient
    public BigDecimal getSubtotalItens() {
        return itens.stream()
                .map(ItemComanda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total a pagar: itens cobrados menos o desconto (nunca negativo). */
    @Transient
    public BigDecimal getTotal() {
        return getSubtotalItens().subtract(getDescontoValor()).max(BigDecimal.ZERO);
    }
}
