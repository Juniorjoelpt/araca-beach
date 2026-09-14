package com.aracabeach.repository;

import com.aracabeach.domain.torneio.Torneio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TorneioRepository extends JpaRepository<Torneio, Long> {
}
