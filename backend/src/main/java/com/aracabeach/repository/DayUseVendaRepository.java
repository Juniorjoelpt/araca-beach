package com.aracabeach.repository;

import com.aracabeach.domain.dayuse.DayUseVenda;
import com.aracabeach.domain.dayuse.StatusDayUse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DayUseVendaRepository extends JpaRepository<DayUseVenda, Long> {
    List<DayUseVenda> findByDataOrderByCriadoEmDesc(LocalDate data);
    List<DayUseVenda> findByDataAndStatus(LocalDate data, StatusDayUse status);
    List<DayUseVenda> findByDataAndStatusIn(LocalDate data, java.util.Collection<StatusDayUse> status);
    List<DayUseVenda> findByClienteIdOrderByDataDescCriadoEmDesc(Long clienteId);
    List<DayUseVenda> findByStatusAndPagoEmBetween(StatusDayUse status, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
    long countByClienteIdAndStatus(Long clienteId, StatusDayUse status);
}
