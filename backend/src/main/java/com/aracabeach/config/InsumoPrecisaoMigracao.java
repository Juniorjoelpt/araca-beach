package com.aracabeach.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Estoque de insumos passou de 3 para 6 casas decimais (fracoes de fardo: 1/24 = 0,041667). O Hibernate nao altera
 * a escala de colunas existentes, entao ajustamos uma unica vez (idempotente; falha so vira log).
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class InsumoPrecisaoMigracao implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        ajustar("rest_insumos", "estoque_atual");
        ajustar("rest_insumos", "estoque_minimo");
        ajustar("rest_movimentos_insumo", "quantidade");
    }

    private void ajustar(String tabela, String coluna) {
        try {
            Integer escala = jdbc.queryForObject(
                    "SELECT NUMERIC_SCALE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", Integer.class, tabela, coluna);
            if (escala != null && escala < 6) {
                jdbc.execute("ALTER TABLE " + tabela + " MODIFY COLUMN " + coluna + " DECIMAL(14,6) NOT NULL");
                log.info("Coluna {}.{} ajustada para DECIMAL(14,6).", tabela, coluna);
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel ajustar {}.{}: {}", tabela, coluna, e.getMessage());
        }
    }
}
