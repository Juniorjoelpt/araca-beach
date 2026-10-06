package com.aracabeach.repository;

import com.aracabeach.domain.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByTelefone(String telefone);
    Optional<Cliente> findByEmail(String email);
    long countByCriadoEmBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
    Optional<Cliente> findByConfirmacaoTokenHash(String confirmacaoTokenHash);
    Optional<Cliente> findByResetTokenHash(String resetTokenHash);
}
