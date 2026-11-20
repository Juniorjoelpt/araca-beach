package com.aracabeach.domain.regra;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dia liberado (feriado): nesse dia as reservas fixas (recorrentes e mensalidades) e as turmas
 * deixam de ocupar a agenda, e o horario fica livre para reserva normal.
 */
@Entity
@Table(name = "dias_liberados")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaLiberado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate data;

    @Column(length = 200)
    private String motivo;

    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
