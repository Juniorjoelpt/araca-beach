package com.aracabeach.domain.comissao;

import com.aracabeach.domain.professor.Professor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lancamento de comissao gerado automaticamente (idempotente por origem + referencia). */
@Entity
@Table(name = "comissao_lancamentos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"origem", "referencia_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComissaoLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemComissao origem;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal base;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private StatusComissao status = StatusComissao.PENDENTE;

    private LocalDate dataPagamento;
}
