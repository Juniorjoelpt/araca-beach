package com.aracabeach.repository;

import com.aracabeach.domain.pacote.PlanoPacote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoPacoteRepository extends JpaRepository<PlanoPacote, Long> {
    List<PlanoPacote> findByAtivoTrueOrderByNomeAsc();
}
