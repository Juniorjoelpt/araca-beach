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
 * day_use_vendas foi criada com status/forma_pagamento como ENUM nativo do MySQL (sem PENDENTE) e forma obrigatoria.
 * A reserva online precisa de PENDENTE e de forma nula. Converte uma unica vez (idempotente) e preenche pago_em das
 * vendas antigas. Falhas viram so log.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class DayUseMigracao implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (tipo("status") != null && tipo("status").toLowerCase().startsWith("enum")) {
                jdbc.execute("ALTER TABLE day_use_vendas MODIFY COLUMN status VARCHAR(20) NOT NULL");
                log.info("day_use_vendas.status convertida de ENUM para VARCHAR(20).");
            }
            String forma = tipo("forma_pagamento");
            boolean obrigatoria = "NO".equals(anulavel("forma_pagamento"));
            if (forma != null && (forma.toLowerCase().startsWith("enum") || obrigatoria)) {
                jdbc.execute("ALTER TABLE day_use_vendas MODIFY COLUMN forma_pagamento VARCHAR(20) NULL");
                log.info("day_use_vendas.forma_pagamento convertida para VARCHAR(20) NULL.");
            }
            if (tipo("pago_em") != null) {
                jdbc.update("UPDATE day_use_vendas SET pago_em = criado_em WHERE status = 'ATIVA' AND pago_em IS NULL");
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel ajustar day_use_vendas: {}", e.getMessage());
        }
    }

    private String tipo(String coluna) {
        List<String> r = jdbc.queryForList("SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = 'day_use_vendas' AND COLUMN_NAME = ?", String.class, coluna);
        return r.isEmpty() ? null : r.get(0);
    }

    private String anulavel(String coluna) {
        List<String> r = jdbc.queryForList("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = 'day_use_vendas' AND COLUMN_NAME = ?", String.class, coluna);
        return r.isEmpty() ? null : r.get(0);
    }
}
