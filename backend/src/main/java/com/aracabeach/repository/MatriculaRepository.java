package com.aracabeach.repository;

import com.aracabeach.domain.matricula.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAtivaTrue();

    List<Matricula> findByTurmaIdAndAtivaTrue(Long turmaId);

    List<Matricula> findByClienteIdAndAtivaTrue(Long clienteId);

    Optional<Matricula> findByTurmaIdAndClienteIdAndAtivaTrue(Long turmaId, Long clienteId);
}
