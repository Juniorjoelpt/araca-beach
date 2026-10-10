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
 * Bancos antigos tem usuarios.perfil como ENUM nativo do MySQL (ADMIN, RECEPCAO), e o Hibernate nao altera
 * ENUM existente - o novo perfil GARCOM seria recusado. Converte para VARCHAR uma unica vez (idempotente).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class UsuarioPerfilMigracao implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> tipos = jdbc.queryForList(
                    "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'perfil'",
                    String.class);
            if (!tipos.isEmpty() && tipos.get(0).toLowerCase().startsWith("enum")) {
                jdbc.execute("ALTER TABLE usuarios MODIFY COLUMN perfil VARCHAR(20) NOT NULL");
                log.info("Coluna usuarios.perfil convertida de ENUM para VARCHAR(20).");
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel ajustar a coluna usuarios.perfil: {}", e.getMessage());
        }
    }
}
