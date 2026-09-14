package com.aracabeach.repository;

import com.aracabeach.domain.despesa.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findByDataVencimentoBetweenOrderByDataVencimento(LocalDate inicio, LocalDate fim);
    List<Despesa> findByPagaFalseOrderByDataVencimento();
}
