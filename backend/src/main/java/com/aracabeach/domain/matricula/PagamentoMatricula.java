package com.aracabeach.domain.matricula;

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
 * Cobranca mensal de uma Matricula - clone exato, para o modulo de Aulas,
 * de PagamentoMensalidade (modulo de Reservas). Mantido como entidade
 * separada (em vez de generalizar Mensalidade para os dois casos) para nao
 * arriscar a funcionalidade de mensalidades de quadra que ja esta em
 * producao.
 */
@Entity
@Table(name = "pagamentos_matricula")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    // "yyyy-MM"
    @Column(nullable = false)
    private String referenciaMes;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Builder.Default
    @Column(nullable = false)
    private boolean pago = false;

    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;
}
