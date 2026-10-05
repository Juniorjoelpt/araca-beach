package com.aracabeach.repository;

import com.aracabeach.domain.matriculacliente.ReservaGeradaPorMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaGeradaPorMatriculaRepository extends JpaRepository<ReservaGeradaPorMatricula, Long> {
    List<ReservaGeradaPorMatricula> findByHorarioIdOrderByDataDesc(Long horarioId);

    List<ReservaGeradaPorMatricula> findByHorarioIdAndDataAfter(Long horarioId, LocalDate data);
}
