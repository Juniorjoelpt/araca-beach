package com.aracabeach.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bancos criados antes de FARDO/PACOTE existirem tem a coluna rest_insumos.unidade como ENUM nativo do
 * MySQL (KG, L, UN), e o Hibernate nao altera ENUM existente. Esta rotina converte a coluna para VARCHAR
 * uma unica vez (idempotente). Qualquer falha e so registrada em log, sem derrubar o sistema.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class InsumoUnidadeMigracao implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> tipos = jdbc.queryForList(
                    "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rest_insumos' AND COLUMN_NAME = 'unidade'",
                    String.class);
            if (!tipos.isEmpty() && tipos.get(0).toLowerCase().startsWith("enum")) {
                jdbc.execute("ALTER TABLE rest_insumos MODIFY COLUMN unidade VARCHAR(10) NOT NULL");
                log.info("Coluna rest_insumos.unidade convertida de ENUM para VARCHAR(10).");
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel ajustar a coluna rest_insumos.unidade: {}", e.getMessage());
        }
    }
}
