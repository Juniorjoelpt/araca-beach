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
 * Venda avulsa: rest_comandas.cliente_id passa a aceitar NULL. O Hibernate (ddl-auto: update) nao remove
 * NOT NULL de coluna existente, entao esta rotina faz o ALTER uma unica vez (idempotente). Falha so vai para o log.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ComandaAvulsaMigracao implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> nulos = jdbc.queryForList(
                    "SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rest_comandas' AND COLUMN_NAME = 'cliente_id'",
                    String.class);
            if (!nulos.isEmpty() && "NO".equalsIgnoreCase(nulos.get(0))) {
                jdbc.execute("ALTER TABLE rest_comandas MODIFY COLUMN cliente_id BIGINT NULL");
                log.info("Coluna rest_comandas.cliente_id agora aceita NULL (venda avulsa).");
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel ajustar a coluna rest_comandas.cliente_id: {}", e.getMessage());
        }
    }
}
