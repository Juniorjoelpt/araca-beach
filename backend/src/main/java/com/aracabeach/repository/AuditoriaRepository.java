package com.aracabeach.repository;

import com.aracabeach.domain.auditoria.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long>,
        JpaSpecificationExecutor<RegistroAuditoria> {

    @Modifying
    @Transactional
    @Query("DELETE FROM RegistroAuditoria r WHERE r.criadoEm < :limite")
    int apagarAnterioresA(@Param("limite") LocalDateTime limite);

    @Query("SELECT DISTINCT r.recurso FROM RegistroAuditoria r WHERE r.recurso IS NOT NULL ORDER BY r.recurso")
    List<String> recursosDistintos();
}
