package com.aracabeach.repository;

import com.aracabeach.domain.estoque.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    List<MovimentacaoEstoque> findByCriadoEmBetweenOrderByCriadoEmDesc(LocalDateTime inicio, LocalDateTime fim);
    List<MovimentacaoEstoque> findByProdutoIdAndCriadoEmBetweenOrderByCriadoEmDesc(Long produtoId, LocalDateTime inicio, LocalDateTime fim);
}
