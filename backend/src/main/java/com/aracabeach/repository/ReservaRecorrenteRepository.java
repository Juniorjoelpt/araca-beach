package com.aracabeach.repository;

import com.aracabeach.domain.reserva.ReservaRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRecorrenteRepository extends JpaRepository<ReservaRecorrente, Long> {
    List<ReservaRecorrente> findByAtivaTrue();

    // Recorrencias "sem data final" sao as unicas que precisam ter o
    // horizonte de geracao estendido periodicamente (ver ReservaRecorrenteScheduler).
    // Quando vigenciaFim e informado, criar() ja gera todas as ocorrencias de uma vez.
    List<ReservaRecorrente> findByAtivaTrueAndVigenciaFimIsNull();
}
