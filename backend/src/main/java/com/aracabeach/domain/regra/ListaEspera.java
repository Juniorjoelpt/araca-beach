package com.aracabeach.domain.regra;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.quadra.Quadra;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lista_espera")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusListaEspera status = StatusListaEspera.AGUARDANDO;

    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime notificadoEm;
}
