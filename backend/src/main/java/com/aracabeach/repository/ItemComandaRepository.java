package com.aracabeach.repository;

import com.aracabeach.domain.produto.ItemComanda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemComandaRepository extends JpaRepository<ItemComanda, Long> {
}
