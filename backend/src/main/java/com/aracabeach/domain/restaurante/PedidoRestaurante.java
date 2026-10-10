package com.aracabeach.domain.restaurante;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Um envio de itens para a cozinha/bar (gera os tickets impressos). */
@Entity
@Table(name = "rest_pedidos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRestaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private ComandaRestaurante comanda;

    /** Sequencia dentro da comanda (1, 2, 3...). */
    @Column(nullable = false)
    private int numero;

    /** Quem lancou o pedido (garcom ou operador). */
    @Column(name = "lancado_por", length = 80)
    private String lancadoPor;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<ItemPedidoRestaurante> itens = new ArrayList<>();
}
