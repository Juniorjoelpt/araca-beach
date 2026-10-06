package com.aracabeach.repository;

import com.aracabeach.domain.pacote.PacoteCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacoteClienteRepository extends JpaRepository<PacoteCliente, Long> {
    List<PacoteCliente> findAllByOrderByDataCompraDescIdDesc();

    List<PacoteCliente> findByClienteIdOrderByDataCompraDescIdDesc(Long clienteId);

    List<PacoteCliente> findByPagoFalseAndCanceladoFalseOrderByDataCompraAsc();
}
