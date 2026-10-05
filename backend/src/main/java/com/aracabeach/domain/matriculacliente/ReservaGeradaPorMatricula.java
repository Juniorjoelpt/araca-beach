package com.aracabeach.domain.matriculacliente;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Registro de qual Reserva (pelo id) foi gerada para qual horario de
 * matricula, em qual data. Existe para nao precisar mexer na entidade
 * Reserva (que ja tem um campo solto "reservaRecorrenteId" usado por outro
 * fluxo - reaproveitar esse mesmo campo aqui criaria risco de colisao de
 * ids entre ReservaRecorrente e MatriculaClienteHorario). Usado para
 * cancelar reservas futuras de um horario e para saber ate quando o
 * horizonte de geracao ja foi preenchido (ver MatriculaClienteService).
 */
@Entity
@Table(name = "reservas_geradas_por_matricula")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaGeradaPorMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_cliente_horario_id", nullable = false)
    private MatriculaClienteHorario horario;

    @Column(nullable = false)
    private Long reservaId;

    @Column(nullable = false)
    private LocalDate data;
}
