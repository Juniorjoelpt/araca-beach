package com.aracabeach.domain.dayuse;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Venda de ingressos de Day Use para um dia de uso. */
@Entity
@Table(name = "day_use_vendas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayUseVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dia em que os ingressos valem. */
    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "nome_cliente", length = 80)
    private String nomeCliente;

    @Column(length = 30)
    private String telefone;

    @Column(length = 200)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    /** Total de pessoas (soma das quantidades dos itens). */
    @Column(nullable = false)
    private int pessoas;

    /** Quantas dessas pessoas ja entraram (check-in na portaria). */
    @Column(nullable = false)
    @Builder.Default
    private int entraram = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusDayUse status = StatusDayUse.ATIVA;

    @Column(name = "pagamento_id")
    private Long pagamentoId;

    @Column(length = 60)
    private String operador;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "motivo_cancelamento", length = 200)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<DayUseItem> itens = new ArrayList<>();
}
