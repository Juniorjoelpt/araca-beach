package com.aracabeach.repository;

import com.aracabeach.domain.turma.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findByAtivaTrue();
}
