package com.aracabeach.domain.matriculacliente;

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
 * Um dos dias/horarios semanais cobertos por uma MatriculaCliente (ex.:
 * "Quadra 1, Terca, 17h-18h"). Uma matricula pode ter varios horarios.
 */
@Entity
@Table(name = "matricula_cliente_horarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatriculaClienteHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_cliente_id", nullable = false)
    private MatriculaCliente matriculaCliente;

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

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;
}
