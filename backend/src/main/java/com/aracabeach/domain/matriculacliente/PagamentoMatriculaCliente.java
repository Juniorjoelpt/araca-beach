package com.aracabeach.domain.matriculacliente;

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
 * Cobranca mensal de uma MatriculaCliente - clone de PagamentoMensalidade,
 * so que a matricula pode cobrir varios horarios de uma vez.
 */
@Entity
@Table(name = "pagamentos_matricula_cliente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoMatriculaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_cliente_id", nullable = false)
    private MatriculaCliente matriculaCliente;

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
