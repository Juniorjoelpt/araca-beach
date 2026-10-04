package com.aracabeach.repository;

import com.aracabeach.domain.mensalidade.PagamentoMensalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoMensalidadeRepository extends JpaRepository<PagamentoMensalidade, Long> {

    List<PagamentoMensalidade> findByMensalidadeIdOrderByVencimentoDesc(Long mensalidadeId);

    Optional<PagamentoMensalidade> findByMensalidadeIdAndReferenciaMes(Long mensalidadeId, String referenciaMes);

    List<PagamentoMensalidade> findByPagoFalseOrderByVencimentoAsc();
}
