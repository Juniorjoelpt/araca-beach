package com.aracabeach.domain.torneio;

import com.aracabeach.domain.quadra.TipoQuadra;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "torneios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoQuadra modalidade;

    @Column(nullable = false)
    private LocalDate dataInicio;

    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusTorneio status = StatusTorneio.INSCRICOES_ABERTAS;

    @ElementCollection
    @CollectionTable(name = "torneio_categorias", joinColumns = @JoinColumn(name = "torneio_id"))
    @Column(name = "categoria")
    @Builder.Default
    private List<String> categorias = new ArrayList<>();
}
