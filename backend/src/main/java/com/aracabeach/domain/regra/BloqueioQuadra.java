package com.aracabeach.domain.regra;

import com.aracabeach.domain.quadra.Quadra;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Bloqueio de agenda (manutencao, evento, clima). Quadra nula = todas as quadras. */
@Entity
@Table(name = "bloqueios_quadra")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueioQuadra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quadra_id")
    private Quadra quadra;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(length = 200)
    private String motivo;
}
