package com.aracabeach.repository;

import com.aracabeach.domain.regra.BloqueioQuadra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioQuadraRepository extends JpaRepository<BloqueioQuadra, Long> {

    @Query("""
        SELECT b FROM BloqueioQuadra b
        WHERE (b.quadra IS NULL OR b.quadra.id = :quadraId)
          AND b.inicio < :fim
          AND b.fim > :inicio
        """)
    List<BloqueioQuadra> findConflitantes(
            @Param("quadraId") Long quadraId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    List<BloqueioQuadra> findByFimAfterOrderByInicioAsc(LocalDateTime agora);

    /** Bloqueios manuais (os de turma ficam de fora da tela de Regras). */
    List<BloqueioQuadra> findByFimAfterAndTurmaIdIsNullOrderByInicioAsc(LocalDateTime agora);

    void deleteByTurmaIdAndFimAfter(Long turmaId, LocalDateTime agora);

    void deleteByTurmaId(Long turmaId);
}
