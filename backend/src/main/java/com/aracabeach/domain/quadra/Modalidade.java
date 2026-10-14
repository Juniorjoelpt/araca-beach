package com.aracabeach.domain.quadra;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modalidade esportiva cadastravel (Vôlei, Futevôlei, Beach Tennis, Altinha...). O codigo e o valor gravado em
 * Quadra.tipo, Professor.especialidade, Torneio.modalidade, Turma.tipo e PlanoPacote.tipo; e gerado a partir do nome
 * na criacao e nunca muda (renomear altera so o nome exibido).
 */
@Entity
@Table(name = "modalidades", uniqueConstraints = @UniqueConstraint(name = "uk_modalidade_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nome;
}
