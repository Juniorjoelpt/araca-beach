package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.PedidoRestaurante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRestauranteRepository extends JpaRepository<PedidoRestaurante, Long> {

    List<PedidoRestaurante> findByImpressaoPendenteTrueAndCriadoEmAfterOrderByCriadoEmAsc(LocalDateTime desde);

    /** Marca como impresso so se ainda estava pendente: garante que dois computadores nao imprimam o mesmo pedido. */
    @Modifying
    @Query("update PedidoRestaurante p set p.impressaoPendente = false where p.id = :id and p.impressaoPendente = true")
    int reivindicarImpressao(@Param("id") Long id);
}
