package com.aracabeach.repository;

import com.aracabeach.domain.torneio.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    List<Inscricao> findByTorneioId(Long torneioId);
}
