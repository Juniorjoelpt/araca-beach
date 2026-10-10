package com.aracabeach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do sistema de gestao da arena Araca Beach.
 * Modulos: Quadras, Reservas, Clientes, Financeiro, Aulas, Torneios e Loja.
 */
@SpringBootApplication
@EnableScheduling
public class AracaBeachApplication {

    public static void main(String[] args) {
        // Garantia extra: mesmo sem a flag -Duser.timezone, o sistema trabalha no horario de Brasilia (Fortaleza).
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Fortaleza"));
        SpringApplication.run(AracaBeachApplication.class, args);
    }
}
