package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rest_movimentos_insumo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentoInsumo tipo;

    /** Variacao assinada do estoque (negativa para consumo/perda). */
    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal quantidade;

    @Column(length = 200)
    private String observacao;

    @Column(name = "comanda_id")
    private Long comandaId;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
