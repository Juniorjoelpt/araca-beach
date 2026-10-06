package com.aracabeach.repository;

import com.aracabeach.domain.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByTelefone(String telefone);
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByResetTokenHash(String resetTokenHash);
}
