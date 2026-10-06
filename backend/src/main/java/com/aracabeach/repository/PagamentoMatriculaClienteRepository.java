package com.aracabeach.repository;

import com.aracabeach.domain.matriculacliente.PagamentoMatriculaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoMatriculaClienteRepository extends JpaRepository<PagamentoMatriculaCliente, Long> {

    List<PagamentoMatriculaCliente> findByMatriculaClienteIdOrderByVencimentoDesc(Long matriculaClienteId);

    Optional<PagamentoMatriculaCliente> findByMatriculaClienteIdAndReferenciaMes(Long matriculaClienteId, String referenciaMes);

    List<PagamentoMatriculaCliente> findByPagoFalseOrderByVencimentoAsc();

    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM PagamentoMatriculaCliente p
        WHERE p.pago = false AND p.matriculaCliente.cliente.id = :clienteId
        ORDER BY p.vencimento ASC
        """)
    List<PagamentoMatriculaCliente> pendentesDoCliente(@org.springframework.data.repository.query.Param("clienteId") Long clienteId);
}
