package com.aracabeach.repository;

import com.aracabeach.domain.reserva.ReservaRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRecorrenteRepository extends JpaRepository<ReservaRecorrente, Long> {
    List<ReservaRecorrente> findByAtivaTrue();
}
