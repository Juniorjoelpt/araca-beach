package com.aracabeach.domain.pacote;

import com.aracabeach.domain.professor.Professor;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.reserva.Reserva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aula agendada (e chamada/presenca) consumindo credito de um PacoteCliente. */
@Entity
@Table(name = "aulas_pacote")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AulaPacote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pacote_id", nullable = false)
    private PacoteCliente pacote;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    /** Reserva que bloqueia a quadra no horario da aula. */
    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusAulaPacote status = StatusAulaPacote.AGENDADA;

    /** Aula de reposicao de outra aula (falta avisada). */
    private Long reposicaoDeAulaId;

    @Column(length = 300)
    private String observacoes;
}
