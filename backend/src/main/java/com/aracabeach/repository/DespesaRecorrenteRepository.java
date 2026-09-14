package com.aracabeach.repository;

import com.aracabeach.domain.despesa.DespesaRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DespesaRecorrenteRepository extends JpaRepository<DespesaRecorrente, Long> {
    List<DespesaRecorrente> findByAtivaTrue();
}
