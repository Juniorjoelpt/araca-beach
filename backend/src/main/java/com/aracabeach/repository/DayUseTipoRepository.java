package com.aracabeach.repository;

import com.aracabeach.domain.dayuse.DayUseTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DayUseTipoRepository extends JpaRepository<DayUseTipo, Long> {
    List<DayUseTipo> findByAtivoTrueOrderByNomeAsc();
    List<DayUseTipo> findAllByOrderByNomeAsc();
}
