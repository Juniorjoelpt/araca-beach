package com.aracabeach.repository;

import com.aracabeach.domain.matricula.PagamentoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoMatriculaRepository extends JpaRepository<PagamentoMatricula, Long> {

    List<PagamentoMatricula> findByMatriculaIdOrderByVencimentoDesc(Long matriculaId);

    Optional<PagamentoMatricula> findByMatriculaIdAndReferenciaMes(Long matriculaId, String referenciaMes);

    List<PagamentoMatricula> findByPagoFalseOrderByVencimentoAsc();

    List<PagamentoMatricula> findByPagoTrueAndDataPagamentoGreaterThanEqual(java.time.LocalDate data);

    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM PagamentoMatricula p
        WHERE p.pago = false AND p.matricula.cliente.id = :clienteId
        ORDER BY p.vencimento ASC
        """)
    List<PagamentoMatricula> pendentesDoCliente(@org.springframework.data.repository.query.Param("clienteId") Long clienteId);
}
