package com.aracabeach.repository;

import com.aracabeach.domain.matriculacliente.MatriculaClienteHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculaClienteHorarioRepository extends JpaRepository<MatriculaClienteHorario, Long> {
    List<MatriculaClienteHorario> findByMatriculaClienteId(Long matriculaClienteId);

    List<MatriculaClienteHorario> findByAtivoTrue();
}
