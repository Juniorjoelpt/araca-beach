package com.aracabeach.repository;

import com.aracabeach.domain.produto.Comanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    List<Comanda> findByFechadaFalse();
    List<Comanda> findByFechadaTrueAndFechadaEmBetween(LocalDateTime inicio, LocalDateTime fim);
    List<Comanda> findByFechadaTrueAndCriadoEmBetween(LocalDateTime inicio, LocalDateTime fim);
    List<Comanda> findByClienteIdOrderByCriadoEmDesc(Long clienteId);
}
