package com.aracabeach.repository;

import com.aracabeach.domain.regra.RegraPreco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegraPrecoRepository extends JpaRepository<RegraPreco, Long> {
    List<RegraPreco> findByAtivaTrue();
}
