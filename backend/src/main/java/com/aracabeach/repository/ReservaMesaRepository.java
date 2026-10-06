package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.ReservaMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaMesaRepository extends JpaRepository<ReservaMesa, Long> {
    List<ReservaMesa> findByDataHoraBetweenOrderByDataHora(LocalDateTime inicio, LocalDateTime fim);
}
