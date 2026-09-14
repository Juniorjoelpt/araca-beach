package com.aracabeach.repository;

import com.aracabeach.domain.financeiro.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByReservaId(Long reservaId);
    List<Pagamento> findByCriadoEmBetween(LocalDateTime inicio, LocalDateTime fim);
}
