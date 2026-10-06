package com.aracabeach.repository;

import com.aracabeach.domain.regra.ListaEspera;
import com.aracabeach.domain.regra.StatusListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    List<ListaEspera> findByClienteIdOrderByInicioAsc(Long clienteId);

    List<ListaEspera> findByStatusInAndFimAfterOrderByCriadoEmAsc(Collection<StatusListaEspera> status, LocalDateTime agora);

    @Query("""
        SELECT l FROM ListaEspera l
        WHERE l.quadra.id = :quadraId
          AND l.status = com.aracabeach.domain.regra.StatusListaEspera.AGUARDANDO
          AND l.inicio < :fim
          AND l.fim > :inicio
        ORDER BY l.criadoEm ASC
        """)
    List<ListaEspera> findAguardandoSobrepostos(
            @Param("quadraId") Long quadraId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT l FROM ListaEspera l
        WHERE l.cliente.id = :clienteId
          AND l.quadra.id = :quadraId
          AND l.status IN (com.aracabeach.domain.regra.StatusListaEspera.AGUARDANDO,
                           com.aracabeach.domain.regra.StatusListaEspera.NOTIFICADO)
          AND l.inicio < :fim
          AND l.fim > :inicio
        """)
    List<ListaEspera> findAbertosDoClienteSobrepostos(
            @Param("clienteId") Long clienteId,
            @Param("quadraId") Long quadraId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
