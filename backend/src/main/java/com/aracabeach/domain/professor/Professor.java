package com.aracabeach.domain.professor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "professores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 20)
    private String telefone;

    /** Codigo da modalidade (ver Modalidade.codigo). */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private String especialidade;

    @Column(name = "percentual_comissao", precision = 5, scale = 2)
    private BigDecimal percentualComissao;

    /** PERCENTUAL (padrao) ou POR_HORA. Nulo = PERCENTUAL (cadastros antigos). */
    @Column(name = "tipo_remuneracao", length = 12)
    private String tipoRemuneracao;

    /** Valor pago por hora/aula quando tipoRemuneracao = POR_HORA. */
    @Column(name = "valor_hora", precision = 10, scale = 2)
    private BigDecimal valorHora;

    public boolean isPorHora() {
        return "POR_HORA".equals(tipoRemuneracao);
    }

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
