package com.aracabeach.domain.torneio;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inscricoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "torneio_id", nullable = false)
    private Torneio torneio;

    @Column(nullable = false, length = 120)
    private String participante;

    /** Para duplas (ex.: beach tennis), nome do parceiro. */
    @Column(length = 120)
    private String parceiro;

    @Column(nullable = false, length = 60)
    private String categoria;
}
