package com.aracabeach.domain.regra;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Configuracao unica (id = 1) das regras de reserva da arena. */
@Entity
@Table(name = "configuracao_reserva")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoReserva {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id;

    /** Cancelamento ate X horas antes do inicio nao paga multa. */
    @Builder.Default
    @Column(nullable = false)
    private int horasCancelamentoGratis = 24;

    /** % do valor da reserva cobrado em cancelamento tardio. */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualMulta = new BigDecimal("50");

    /** % do valor da reserva cobrado quando o cliente nao comparece. */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualMultaNoShow = new BigDecimal("100");

    /** Desconto (%) para clientes com mensalidade ativa. */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal descontoMensalistaPercentual = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean listaEsperaAtiva = true;

    /** Aviso de falta de aula de pacote ate X horas antes devolve o credito. */
    @Builder.Default
    @Column(nullable = false)
    private int horasAvisoFaltaAula = 12;
}
