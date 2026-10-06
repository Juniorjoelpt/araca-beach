package com.aracabeach.domain.restaurante;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rest_ficha_tecnica")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichaTecnicaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemCardapio item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    /** Quantidade do insumo (na unidade dele) consumida por 1 unidade vendida do item. */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantidade;
}
