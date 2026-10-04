package com.aracabeach.domain.mensalidade;

import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cobranca de um mes especifico de uma Mensalidade. Gerada automaticamente
 * pelo MensalidadeScheduler e marcada como paga quando a recepcao registra
 * o recebimento.
 */
@Entity
@Table(name = "pagamentos_mensalidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoMensalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mensalidade_id", nullable = false)
    private Mensalidade mensalidade;

    /** Formato "yyyy-MM". */
    @Column(name = "referencia_mes", nullable = false, length = 7)
    private String referenciaMes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false)
    @Builder.Default
    private boolean pago = false;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamento formaPagamento;
}
