package com.aracabeach.repository;

import com.aracabeach.domain.comissao.ComissaoLancamento;
import com.aracabeach.domain.comissao.OrigemComissao;
import com.aracabeach.domain.comissao.StatusComissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComissaoLancamentoRepository extends JpaRepository<ComissaoLancamento, Long> {

    Optional<ComissaoLancamento> findByOrigemAndReferenciaId(OrigemComissao origem, Long referenciaId);

    @Query("""
        SELECT c FROM ComissaoLancamento c
        WHERE (:professorId IS NULL OR c.professor.id = :professorId)
          AND (:status IS NULL OR c.status = :status)
          AND c.competencia BETWEEN :inicio AND :fim
        ORDER BY c.competencia DESC, c.id DESC
        """)
    List<ComissaoLancamento> buscar(
            @Param("professorId") Long professorId,
            @Param("status") StatusComissao status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}
