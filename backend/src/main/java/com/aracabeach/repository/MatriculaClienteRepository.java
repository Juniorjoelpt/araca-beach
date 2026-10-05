package com.aracabeach.repository;

import com.aracabeach.domain.matriculacliente.MatriculaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculaClienteRepository extends JpaRepository<MatriculaCliente, Long> {
    List<MatriculaCliente> findByAtivaTrue();
}
