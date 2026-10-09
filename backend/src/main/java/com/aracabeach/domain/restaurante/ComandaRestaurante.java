package com.aracabeach.domain.restaurante;

import com.aracabeach.domain.cliente.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comanda do restaurante. Com reserva de mesa, a comanda fica atrelada a mesa
 * (reservaMesa preenchida); sem reserva, fica atrelada ao cliente (a mesa e so um
 * rotulo opcional para o servico).
 */
@Entity
@Table(name = "rest_comandas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComandaRestaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Bloqueio otimista: impede fechar a mesma comanda duas vezes ao mesmo tempo. */
    @Version
    private Long versao;

    /** Nulo em venda avulsa (balcao), que nao exige cliente cadastrado. */
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Nome/apelido livre da venda avulsa (ex.: "Balcao", "Moto azul"). Opcional. */
    @Column(name = "nome_avulso", length = 80)
    private String nomeAvulso;

    @ManyToOne
    @JoinColumn(name = "reserva_mesa_id")
    private ReservaMesa reservaMesa;

    @Column(length = 40)
    private String mesa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusComandaRestaurante status = StatusComandaRestaurante.ABERTA;

    /** Percentual de taxa de servico aplicado (0 = sem taxa). */
    @Column(name = "taxa_servico_percentual", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxaServicoPercentual = BigDecimal.ZERO;

    @Column(name = "desconto_valor", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descontoValor = BigDecimal.ZERO;

    @Column(name = "desconto_motivo", length = 200)
    private String descontoMotivo;

    @Column(name = "aberta_em", nullable = false)
    @Builder.Default
    private LocalDateTime abertaEm = LocalDateTime.now();

    @Column(name = "fechada_em")
    private LocalDateTime fechadaEm;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<PedidoRestaurante> pedidos = new ArrayList<>();

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<PagamentoComandaRestaurante> pagamentos = new ArrayList<>();
}
