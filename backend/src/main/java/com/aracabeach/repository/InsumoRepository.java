package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    List<Insumo> findAllByOrderByNomeAsc();
}
