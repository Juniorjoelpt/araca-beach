package com.aracabeach.config;

import com.aracabeach.domain.usuario.Perfil;
import com.aracabeach.domain.usuario.Usuario;
import com.aracabeach.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Garante que sempre exista pelo menos um usuario administrador ao subir a aplicacao.
 * So cria o admin padrao se a tabela de usuarios estiver vazia - nao sobrescreve
 * nem duplica usuarios ja cadastrados.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = Usuario.builder()
                .nome("Administrador")
                .login("admin")
                .senhaHash(passwordEncoder.encode("admin123"))
                .perfil(Perfil.ADMIN)
                .ativo(true)
                .build();

        usuarioRepository.save(admin);
        log.info("Usuário administrador padrão criado (login: admin / senha: admin123) - troque a senha após o primeiro acesso.");
    }
}
