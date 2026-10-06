package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.FichaTecnicaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FichaTecnicaItemRepository extends JpaRepository<FichaTecnicaItem, Long> {
    List<FichaTecnicaItem> findByItemId(Long itemId);
    boolean existsByInsumoId(Long insumoId);
}
