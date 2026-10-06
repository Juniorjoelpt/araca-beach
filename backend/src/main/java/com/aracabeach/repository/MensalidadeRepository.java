package com.aracabeach.repository;

import com.aracabeach.domain.mensalidade.Mensalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long> {
    List<Mensalidade> findByAtivaTrue();

    boolean existsByAtivaTrueAndReservaRecorrenteClienteId(Long clienteId);

    Optional<Mensalidade> findByReservaRecorrenteId(Long reservaRecorrenteId);
}
