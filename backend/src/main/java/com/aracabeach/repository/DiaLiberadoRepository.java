package com.aracabeach.repository;

import com.aracabeach.domain.regra.DiaLiberado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DiaLiberadoRepository extends JpaRepository<DiaLiberado, Long> {

    boolean existsByData(LocalDate data);

    List<DiaLiberado> findByDataGreaterThanEqualOrderByDataAsc(LocalDate data);
}
