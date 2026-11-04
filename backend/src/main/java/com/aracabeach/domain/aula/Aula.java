package com.aracabeach.domain.aula;

import com.aracabeach.domain.professor.Professor;
import com.aracabeach.domain.quadra.Quadra;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aulas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoAula tipo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @ElementCollection
    @CollectionTable(name = "aula_alunos", joinColumns = @JoinColumn(name = "aula_id"))
    @Column(name = "nome_aluno")
    @Builder.Default
    private List<String> alunos = new ArrayList<>();

    @Column(precision = 10, scale = 2)
    /** Valor total da aula (= valorPorAluno x alunos, minimo 1 aluno). */
    private java.math.BigDecimal valor;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal valorPorAluno;

    /** Recebimento da aula avulsa (entra no caixa quando marcado como pago). */
    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean pago = false;

    private java.time.LocalDate dataPagamento;

    /** Aula com alunos individuais (participantes), cada um com seu pagamento. */
    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean porAluno = false;

    @OneToMany(mappedBy = "aula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    @Builder.Default
    private List<AulaParticipante> participantes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(length = 20)
    private com.aracabeach.domain.financeiro.FormaPagamento formaPagamento;
}
