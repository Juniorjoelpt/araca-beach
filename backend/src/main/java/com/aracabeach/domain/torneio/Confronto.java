package com.aracabeach.domain.torneio;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma partida dentro do chaveamento de eliminacao simples de uma categoria
 * de um torneio. "rodada" comeca em 1 (primeira fase) e sobe ate a final.
 * "posicao" e o indice (0-based) da partida dentro da rodada - usado para
 * calcular automaticamente para qual partida da proxima rodada o vencedor avanca
 * (rodada+1, posicao/2, slot A se posicao par / slot B se posicao impar).
 */
@Entity
@Table(name = "confrontos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Confronto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "torneio_id", nullable = false)
    private Torneio torneio;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false)
    private int rodada;

    @Column(nullable = false)
    private int posicao;

    @Column(name = "participante_a", length = 150)
    private String participanteA;

    @Column(name = "participante_b", length = 150)
    private String participanteB;

    @Column(length = 150)
    private String vencedor;

    @Column(length = 60)
    private String placar;
}
