package com.aracabeach.repository;

import com.aracabeach.domain.quadra.Modalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModalidadeRepository extends JpaRepository<Modalidade, Long> {

    List<Modalidade> findAllByOrderByNomeAsc();

    boolean existsByCodigo(String codigo);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
