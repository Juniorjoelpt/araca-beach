package com.aracabeach.repository;

import com.aracabeach.domain.torneio.Confronto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfrontoRepository extends JpaRepository<Confronto, Long> {

    List<Confronto> findByTorneioIdAndCategoriaOrderByRodadaAscPosicaoAsc(Long torneioId, String categoria);

    Optional<Confronto> findByTorneioIdAndCategoriaAndRodadaAndPosicao(
            Long torneioId, String categoria, int rodada, int posicao);

    void deleteByTorneioIdAndCategoria(Long torneioId, String categoria);
}
