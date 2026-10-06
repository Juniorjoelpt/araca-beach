package com.aracabeach.domain.pacote;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pacote comprado por um cliente. O saldo e calculado a partir das aulas. */
@Entity
@Table(name = "pacotes_cliente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacoteCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoPacote plano;

    @Column(nullable = false)
    private int aulasTotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate dataCompra;

    @Column(nullable = false)
    private LocalDate validade;

    @Builder.Default
    @Column(nullable = false)
    private boolean pago = false;

    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FormaPagamento formaPagamento;

    @Builder.Default
    @Column(nullable = false)
    private boolean cancelado = false;
}
