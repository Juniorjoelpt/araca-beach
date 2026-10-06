package com.aracabeach.domain.regra;

import com.aracabeach.domain.quadra.Quadra;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Preco por horario (ex.: pico 18h-22h). Quadra nula = vale para todas.
 * diasSemana: lista separada por virgula de DayOfWeek (MONDAY,TUESDAY...);
 * vazio = todos os dias. Quando varias regras valem para o mesmo horario,
 * vence a da quadra especifica e, entre iguais, a mais recente (maior id).
 */
@Entity
@Table(name = "regras_preco")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegraPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @ManyToOne
    @JoinColumn(name = "quadra_id")
    private Quadra quadra;

    @Column(length = 100)
    private String diasSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorHora;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativa = true;
}
