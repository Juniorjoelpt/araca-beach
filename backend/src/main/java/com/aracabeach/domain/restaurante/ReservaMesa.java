package com.aracabeach.domain.restaurante;

import com.aracabeach.domain.cliente.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Reserva de mesa do restaurante. O rotulo da mesa (ex.: "Mesa 7") e livre: nao ha numero fixo de mesas. */
@Entity
@Table(name = "rest_reservas_mesa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int pessoas;

    @Column(length = 40)
    private String mesa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusReservaMesa status = StatusReservaMesa.CONFIRMADA;

    @Column(length = 300)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
