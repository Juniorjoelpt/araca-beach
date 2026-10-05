package com.aracabeach.domain.matricula;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.turma.Turma;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vincula um Cliente a uma Turma e carrega os dados de cobranca mensal -
 * e o equivalente, no modulo de Aulas, de "ReservaRecorrente + Mensalidade"
 * do modulo de Reservas (o mesmo cliente so pode ter uma matricula ATIVA por
 * turma; isso e validado em MatriculaService, nao aqui). ultimoMesGerado
 * segue o mesmo padrao de Mensalidade/DespesaRecorrente para a geracao
 * idempotente da cobranca do mes (ver MatriculaService.gerarCobrancasDoMes).
 */
@Entity
@Table(name = "matriculas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate dataMatricula;

    @Column(nullable = false)
    private BigDecimal valorMensal;

    @Column(nullable = false)
    private int diaVencimento;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativa = true;

    // "yyyy-MM" do ultimo mes em que a cobranca ja foi gerada.
    private String ultimoMesGerado;
}
