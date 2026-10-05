package com.aracabeach.domain.turma;

import com.aracabeach.domain.professor.Professor;
import com.aracabeach.domain.quadra.Quadra;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Uma turma e uma aula em grupo que se repete toda semana no mesmo dia e
 * horario (ex.: "Vôlei Infantil - Terças e Quintas às 17h"). Diferente de
 * ReservaRecorrente (que e vinculada a UM cliente), uma Turma nao tem
 * cliente proprio: os alunos entram atraves de Matricula (ManyToOne para
 * Turma, uma por aluno). E o analogo, no modulo de Aulas, da Quadra+horario
 * fixo de uma ReservaRecorrente.
 */
@Entity
@Table(name = "turmas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // Modalidade esportiva da turma (ex.: "VOLEI", "FUTEVOLEI", "BEACH_TENNIS"),
    // mesmos valores usados em Quadra.tipo/Professor.especialidade.
    @Column(nullable = false)
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    // Nulo = sem limite de vagas.
    private Integer capacidadeMaxima;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativa = true;
}
