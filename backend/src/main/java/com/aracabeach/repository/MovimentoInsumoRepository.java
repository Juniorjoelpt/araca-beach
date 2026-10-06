package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.MovimentoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentoInsumoRepository extends JpaRepository<MovimentoInsumo, Long> {
    List<MovimentoInsumo> findTop50ByInsumoIdOrderByCriadoEmDesc(Long insumoId);
}
