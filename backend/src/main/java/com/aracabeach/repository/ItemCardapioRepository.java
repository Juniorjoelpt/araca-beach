package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.ItemCardapio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapio, Long> {
    List<ItemCardapio> findByCategoriaId(Long categoriaId);

    Optional<ItemCardapio> findByCodigoBarras(String codigoBarras);
}
