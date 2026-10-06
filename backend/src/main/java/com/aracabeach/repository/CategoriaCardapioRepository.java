package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.CategoriaCardapio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaCardapioRepository extends JpaRepository<CategoriaCardapio, Long> {
    List<CategoriaCardapio> findAllByOrderByOrdemAscNomeAsc();
}
