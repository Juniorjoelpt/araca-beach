package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    List<Insumo> findAllByOrderByNomeAsc();
    Optional<Insumo> findFirstByNomeIgnoreCase(String nome);
}
