package com.aracabeach.repository;

import com.aracabeach.domain.aula.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface AulaRepository extends JpaRepository<Aula, Long> {

    boolean existsByProfessorId(Long professorId);

    List<Aula> findByInicioBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Aula> findByProfessorIdAndInicioBetween(Long professorId, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
        SELECT a FROM Aula a
        WHERE a.professor.id = :professorId
          AND a.inicio < :fim
          AND a.fim > :inicio
        """)
    List<Aula> findConflitantesDoProfessor(
            @Param("professorId") Long professorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /** Trava a linha da aula: dois cliques simultaneos em Receber nao lancam o pagamento duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Aula a WHERE a.id = :id")
    Optional<Aula> buscarParaEscrita(@Param("id") Long id);
}
