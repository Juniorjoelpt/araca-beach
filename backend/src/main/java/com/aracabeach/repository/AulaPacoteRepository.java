package com.aracabeach.repository;

import com.aracabeach.domain.pacote.AulaPacote;
import com.aracabeach.domain.pacote.StatusAulaPacote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AulaPacoteRepository extends JpaRepository<AulaPacote, Long> {

    List<AulaPacote> findByPacoteIdOrderByInicioAsc(Long pacoteId);

    List<AulaPacote> findByPacoteClienteIdOrderByInicioDesc(Long clienteId);

    List<AulaPacote> findByInicioBetweenOrderByInicioAsc(LocalDateTime inicio, LocalDateTime fim);

    @Query("""
        SELECT COUNT(a) FROM AulaPacote a
        WHERE a.pacote.id = :pacoteId
          AND a.status IN (com.aracabeach.domain.pacote.StatusAulaPacote.AGENDADA,
                           com.aracabeach.domain.pacote.StatusAulaPacote.REALIZADA,
                           com.aracabeach.domain.pacote.StatusAulaPacote.FALTA_SEM_AVISO)
        """)
    long contarConsumidas(@Param("pacoteId") Long pacoteId);

    @Query("""
        SELECT a FROM AulaPacote a
        WHERE a.professor.id = :professorId
          AND a.status = com.aracabeach.domain.pacote.StatusAulaPacote.AGENDADA
          AND a.inicio < :fim
          AND a.fim > :inicio
        """)
    List<AulaPacote> findConflitantesDoProfessor(
            @Param("professorId") Long professorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
