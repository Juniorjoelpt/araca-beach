package com.aracabeach.domain.dayuse;

import jakarta.persistence.*;
import lombok.*;

/** Configuracao unica do Day Use (id = 1). */
@Entity
@Table(name = "day_use_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayUseConfig {

    @Id
    private Long id;

    /** Maximo de pessoas por dia. 0 ou nulo = sem limite. */
    @Column(name = "capacidade_diaria")
    private Integer capacidadeDiaria;
}
