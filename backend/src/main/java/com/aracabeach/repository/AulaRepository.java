package com.aracabeach.repository;

import com.aracabeach.domain.aula.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AulaRepository extends JpaRepository<Aula, Long> {

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
}
