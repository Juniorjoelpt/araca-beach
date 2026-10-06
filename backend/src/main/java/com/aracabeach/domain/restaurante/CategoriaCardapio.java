package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rest_categorias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaCardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    @Builder.Default
    private int ordem = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;
}
