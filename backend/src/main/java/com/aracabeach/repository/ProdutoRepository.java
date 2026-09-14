package com.aracabeach.repository;

import com.aracabeach.domain.produto.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByEstoqueLessThanEqual(Integer limite);
}
