package com.aracabeach.repository;

import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.quadra.id = :quadraId
          AND r.status <> com.aracabeach.domain.reserva.StatusReserva.CANCELADA
          AND r.inicio < :fim
          AND r.fim > :inicio
        """)
    List<Reserva> findConflitantes(
            @Param("quadraId") Long quadraId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    List<Reserva> findByQuadraIdAndInicioBetween(Long quadraId, LocalDateTime inicio, LocalDateTime fim);

    List<Reserva> findByInicioBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Reserva> findByReservaRecorrenteId(Long reservaRecorrenteId);

    List<Reserva> findByClienteIdOrderByInicioDesc(Long clienteId);

    List<Reserva> findByStatus(StatusReserva status);

    List<Reserva> findByStatusAndLembreteEnviadoFalseAndInicioBetween(
            StatusReserva status, LocalDateTime inicio, LocalDateTime fim);
}
