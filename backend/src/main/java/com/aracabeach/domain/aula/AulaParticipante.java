package com.aracabeach.domain.aula;

import com.aracabeach.domain.financeiro.FormaPagamento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Aluno de uma aula avulsa, com valor e pagamento proprios (cada pagamento entra no caixa). */
@Entity
@Table(name = "aula_participantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AulaParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean pago = false;

    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private FormaPagamento formaPagamento;
}
