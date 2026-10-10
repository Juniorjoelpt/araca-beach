package com.aracabeach.repository;

import com.aracabeach.domain.restaurante.ComandaRestaurante;
import com.aracabeach.domain.restaurante.StatusComandaRestaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComandaRestauranteRepository extends JpaRepository<ComandaRestaurante, Long> {
    List<ComandaRestaurante> findByAbertaEmBetweenOrderByAbertaEmDesc(LocalDateTime inicio, LocalDateTime fim);
    List<ComandaRestaurante> findByStatusOrderByAbertaEmAsc(StatusComandaRestaurante status);
    List<ComandaRestaurante> findByStatusAndFechadaEmBetween(StatusComandaRestaurante status, LocalDateTime inicio, LocalDateTime fim);
    List<ComandaRestaurante> findByCortesiaTrueAndStatusAndFechadaEmBetween(StatusComandaRestaurante status, LocalDateTime inicio, LocalDateTime fim);
    Optional<ComandaRestaurante> findFirstByReservaMesaIdAndStatusNot(Long reservaMesaId, StatusComandaRestaurante status);
    List<ComandaRestaurante> findByReservaMesaIdIn(java.util.Collection<Long> ids);
}
