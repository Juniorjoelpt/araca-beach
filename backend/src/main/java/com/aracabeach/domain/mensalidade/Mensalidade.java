package com.aracabeach.domain.mensalidade;

import com.aracabeach.domain.reserva.ReservaRecorrente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plano de mensalidade vinculado a uma ReservaRecorrente: em vez de cobrar
 * cada sessao avulsamente, o cliente paga um valor fixo por mes que cobre
 * todas as ocorrencias da recorrencia. Um job agendado gera automaticamente
 * a cobranca (PagamentoMensalidade) do mes, marcando ultimoMesGerado para
 * nao duplicar o lancamento - mesmo padrao usado em DespesaRecorrente.
 */
@Entity
@Table(name = "mensalidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "reserva_recorrente_id", nullable = false, unique = true)
    private ReservaRecorrente reservaRecorrente;

    @Column(name = "valor_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;

    /** Formato "yyyy-MM" do ultimo mes em que a cobranca foi gerada automaticamente. */
    @Column(name = "ultimo_mes_gerado", length = 7)
    private String ultimoMesGerado;

    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
