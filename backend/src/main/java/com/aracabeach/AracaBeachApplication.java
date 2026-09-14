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
        SpringApplication.run(AracaBeachApplication.class, args);
    }
}
