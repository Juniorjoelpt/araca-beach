package com.aracabeach.domain.quadra;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "quadras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quadra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    /** Codigo da modalidade (ver Modalidade.codigo), ex.: BEACH_TENNIS. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    @NotBlank
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusQuadra status = StatusQuadra.DISPONIVEL;

    @Column(name = "valor_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorHora;

    private Integer capacidade;

    @Column(name = "foto_url", length = 300)
    private String fotoUrl;

    @Column(length = 300)
    private String observacoes;
}
