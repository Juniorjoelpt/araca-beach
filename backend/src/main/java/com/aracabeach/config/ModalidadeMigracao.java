package com.aracabeach.config;

import com.aracabeach.domain.quadra.Modalidade;
import com.aracabeach.repository.ModalidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * As modalidades eram um ENUM Java (e ENUM nativo do MySQL em quadras, professores e torneios). Agora sao cadastraveis:
 * converte essas colunas para VARCHAR (idempotente), cria as tres modalidades de sempre e registra qualquer codigo
 * ja usado nos cadastros. Falhas viram so log.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ModalidadeMigracao implements ApplicationRunner {

    private static final String[][] COLUNAS = {
            {"quadras", "tipo"}, {"professores", "especialidade"}, {"torneios", "modalidade"}};

    private final JdbcTemplate jdbc;
    private final ModalidadeRepository modalidadeRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] c : COLUNAS) {
            try {
                List<String> tipo = jdbc.queryForList("SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", String.class, c[0], c[1]);
                if (!tipo.isEmpty() && tipo.get(0).toLowerCase().startsWith("enum")) {
                    jdbc.execute("ALTER TABLE " + c[0] + " MODIFY COLUMN " + c[1] + " VARCHAR(20) NOT NULL");
                    log.info("{}.{} convertida de ENUM para VARCHAR(20).", c[0], c[1]);
                }
            } catch (Exception e) {
                log.warn("Nao foi possivel converter {}.{}: {}", c[0], c[1], e.getMessage());
            }
        }
        try {
            if (modalidadeRepository.count() == 0) {
                modalidadeRepository.save(Modalidade.builder().codigo("VOLEI").nome("Vôlei").build());
                modalidadeRepository.save(Modalidade.builder().codigo("FUTEVOLEI").nome("Futevôlei").build());
                modalidadeRepository.save(Modalidade.builder().codigo("BEACH_TENNIS").nome("Beach Tennis").build());
                log.info("Modalidades padrao criadas.");
            }
            String[][] usos = {{"quadras", "tipo"}, {"professores", "especialidade"}, {"torneios", "modalidade"},
                    {"turmas", "tipo"}, {"planos_pacote", "tipo"}};
            for (String[] u : usos) {
                List<String> codigos;
                try {
                    codigos = jdbc.queryForList("SELECT DISTINCT " + u[1] + " FROM " + u[0] + " WHERE " + u[1] + " IS NOT NULL AND " + u[1] + " <> ''", String.class);
                } catch (Exception e) {
                    continue; // tabela com outro nome: ignora
                }
                for (String codigo : codigos) {
                    if (!modalidadeRepository.existsByCodigo(codigo)) {
                        modalidadeRepository.save(Modalidade.builder().codigo(codigo).nome(titulo(codigo)).build());
                        log.info("Modalidade '{}' registrada a partir dos cadastros existentes.", codigo);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel preparar as modalidades: {}", e.getMessage());
        }
    }

    private static String titulo(String codigo) {
        StringBuilder sb = new StringBuilder();
        for (String p : codigo.toLowerCase(Locale.ROOT).split("_")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
