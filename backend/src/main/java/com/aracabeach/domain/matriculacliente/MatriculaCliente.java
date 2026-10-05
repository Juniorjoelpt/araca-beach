package com.aracabeach.domain.matriculacliente;

import com.aracabeach.domain.cliente.Cliente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Matricula de um cliente com cobranca mensal UNICA que pode cobrir VARIOS
 * horarios semanais de quadra ao mesmo tempo (ver MatriculaClienteHorario) -
 * por exemplo, um aluno que joga terca E quinta às 17h paga um unico valor
 * mensal cobrindo os dois dias, em vez de precisar de uma Mensalidade por
 * ReservaRecorrente (que so aceita 1 dia/horario cada).
 *
 * Construida como um fluxo PARALELO a ReservaRecorrente/Mensalidade (sem
 * alterar essas entidades, que ja estao em producao): cada horario gera
 * suas proprias ocorrencias de Reserva diretamente (ver
 * MatriculaClienteService), seguindo o mesmo padrao de geracao e extensao
 * de horizonte que ReservaRecorrenteService ja usa.
 */
@Entity
@Table(name = "matriculas_cliente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatriculaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private BigDecimal valorMensal;

    @Column(nullable = false)
    private int diaVencimento;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativa = true;

    // "yyyy-MM" do ultimo mes em que a cobranca ja foi gerada.
    private String ultimoMesGerado;
}
