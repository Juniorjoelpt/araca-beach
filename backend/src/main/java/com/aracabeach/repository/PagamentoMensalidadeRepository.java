package com.aracabeach.repository;

import com.aracabeach.domain.mensalidade.PagamentoMensalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoMensalidadeRepository extends JpaRepository<PagamentoMensalidade, Long> {

    List<PagamentoMensalidade> findByMensalidadeIdOrderByVencimentoDesc(Long mensalidadeId);

    Optional<PagamentoMensalidade> findByMensalidadeIdAndReferenciaMes(Long mensalidadeId, String referenciaMes);

    List<PagamentoMensalidade> findByPagoFalseOrderByVencimentoAsc();

    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM PagamentoMensalidade p
        WHERE p.pago = false AND p.mensalidade.reservaRecorrente.cliente.id = :clienteId
        ORDER BY p.vencimento ASC
        """)
    List<PagamentoMensalidade> pendentesDoCliente(@org.springframework.data.repository.query.Param("clienteId") Long clienteId);
}
