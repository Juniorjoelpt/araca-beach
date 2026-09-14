package com.aracabeach.service;

import com.aracabeach.domain.torneio.Confronto;
import com.aracabeach.domain.torneio.Inscricao;
import com.aracabeach.domain.torneio.StatusTorneio;
import com.aracabeach.domain.torneio.Torneio;
import com.aracabeach.dto.ConfrontoResponse;
import com.aracabeach.dto.RegistrarResultadoRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ConfrontoRepository;
import com.aracabeach.repository.InscricaoRepository;
import com.aracabeach.repository.TorneioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gera e conduz um chaveamento de eliminacao simples para uma categoria
 * de um torneio.
 *
 * Algoritmo de geracao (evita que dois "byes" caiam na mesma partida):
 * 1. Embaralha os participantes e os posiciona nos primeiros N slots de
 *    um vetor de tamanho igual a proxima potencia de 2 (os slots restantes
 *    ficam vazios = bye).
 * 2. Emparelha o slot k com o slot (tamanho-1-k). Como os participantes
 *    ocupam sempre os primeiros N slots e os byes os ultimos, esse
 *    espelhamento garante que um slot vazio nunca encontra outro slot
 *    vazio - o bye sempre cai contra um participante real, que avanca
 *    automaticamente (W.O.).
 * 3. As rodadas seguintes sao pre-criadas vazias; o vencedor de cada
 *    confronto (por W.O. ou resultado registrado depois) e propagado
 *    automaticamente para a partida correspondente na proxima rodada.
 */
@Service
@RequiredArgsConstructor
public class ChaveamentoService {

    private final ConfrontoRepository confrontoRepository;
    private final TorneioRepository torneioRepository;
    private final InscricaoRepository inscricaoRepository;

    @Transactional
    public List<ConfrontoResponse> gerar(Long torneioId, String categoria) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Torneio não encontrado: " + torneioId));

        List<String> participantes = inscricaoRepository.findByTorneioId(torneioId).stream()
                .filter(i -> i.getCategoria().equals(categoria))
                .map(this::nomeExibicao)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (participantes.size() < 2) {
            throw new IllegalArgumentException("A categoria precisa de pelo menos 2 inscritos para gerar o chaveamento.");
        }

        confrontoRepository.deleteByTorneioIdAndCategoria(torneioId, categoria);
        Collections.shuffle(participantes);

        int tamanho = proximaPotenciaDeDois(participantes.size());
        String[] slots = new String[tamanho];
        for (int i = 0; i < participantes.size(); i++) {
            slots[i] = participantes.get(i);
        }

        int totalRodadas = Integer.numberOfTrailingZeros(tamanho);
        List<Confronto> rodada1 = new ArrayList<>();
        for (int k = 0; k < tamanho / 2; k++) {
            String a = slots[k];
            String b = slots[tamanho - 1 - k];
            Confronto confronto = Confronto.builder()
                    .torneio(torneio)
                    .categoria(categoria)
                    .rodada(1)
                    .posicao(k)
                    .participanteA(a)
                    .participanteB(b)
                    .vencedor(a != null && b == null ? a : (b != null && a == null ? b : null))
                    .placar(a != null && b == null || b != null && a == null ? "W.O." : null)
                    .build();
            rodada1.add(confronto);
        }
        confrontoRepository.saveAll(rodada1);

        // pre-cria as rodadas seguintes vazias
        for (int rodada = 2; rodada <= totalRodadas; rodada++) {
            int qtdPartidas = tamanho / (int) Math.pow(2, rodada);
            List<Confronto> partidasRodada = new ArrayList<>();
            for (int posicao = 0; posicao < qtdPartidas; posicao++) {
                partidasRodada.add(Confronto.builder()
                        .torneio(torneio)
                        .categoria(categoria)
                        .rodada(rodada)
                        .posicao(posicao)
                        .build());
            }
            confrontoRepository.saveAll(partidasRodada);
        }

        // propaga os "byes" da rodada 1 (vencedor automatico) para a rodada 2
        for (Confronto c : rodada1) {
            if (c.getVencedor() != null) {
                propagarVencedor(torneioId, categoria, c.getRodada(), c.getPosicao(), c.getVencedor(), totalRodadas);
            }
        }

        return listar(torneioId, categoria);
    }

    @Transactional(readOnly = true)
    public List<ConfrontoResponse> listar(Long torneioId, String categoria) {
        return confrontoRepository.findByTorneioIdAndCategoriaOrderByRodadaAscPosicaoAsc(torneioId, categoria).stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional
    public List<ConfrontoResponse> registrarResultado(Long confrontoId, RegistrarResultadoRequest request) {
        Confronto confronto = confrontoRepository.findById(confrontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Confronto não encontrado: " + confrontoId));

        if (!request.vencedor().equals(confronto.getParticipanteA()) && !request.vencedor().equals(confronto.getParticipanteB())) {
            throw new IllegalArgumentException("O vencedor precisa ser um dos participantes deste confronto.");
        }

        confronto.setVencedor(request.vencedor());
        confronto.setPlacar(request.placar());
        confrontoRepository.save(confronto);

        int totalRodadas = confrontoRepository.findByTorneioIdAndCategoriaOrderByRodadaAscPosicaoAsc(
                        confronto.getTorneio().getId(), confronto.getCategoria()).stream()
                .mapToInt(Confronto::getRodada).max().orElse(confronto.getRodada());

        if (confronto.getRodada() == totalRodadas) {
            Torneio torneio = confronto.getTorneio();
            torneio.setStatus(StatusTorneio.FINALIZADO);
            torneioRepository.save(torneio);
        } else {
            propagarVencedor(confronto.getTorneio().getId(), confronto.getCategoria(),
                    confronto.getRodada(), confronto.getPosicao(), request.vencedor(), totalRodadas);
        }

        return listar(confronto.getTorneio().getId(), confronto.getCategoria());
    }

    /** Preenche o vencedor no slot correto da proxima rodada e, se isso fechar outro W.O., propaga em cascata. */
    private void propagarVencedor(Long torneioId, String categoria, int rodadaAtual, int posicaoAtual, String vencedor, int totalRodadas) {
        if (rodadaAtual >= totalRodadas) return;

        int proximaRodada = rodadaAtual + 1;
        int proximaPosicao = posicaoAtual / 2;
        boolean slotA = posicaoAtual % 2 == 0;

        Confronto proximo = confrontoRepository
                .findByTorneioIdAndCategoriaAndRodadaAndPosicao(torneioId, categoria, proximaRodada, proximaPosicao)
                .orElse(null);
        if (proximo == null) return;

        if (slotA) {
            proximo.setParticipanteA(vencedor);
        } else {
            proximo.setParticipanteB(vencedor);
        }
        confrontoRepository.save(proximo);
    }

    private String nomeExibicao(Inscricao inscricao) {
        return inscricao.getParceiro() != null && !inscricao.getParceiro().isBlank()
                ? inscricao.getParticipante() + " / " + inscricao.getParceiro()
                : inscricao.getParticipante();
    }

    private int proximaPotenciaDeDois(int n) {
        int potencia = 1;
        while (potencia < n) potencia *= 2;
        return potencia;
    }

    private ConfrontoResponse paraResponse(Confronto c) {
        return new ConfrontoResponse(c.getId(), c.getRodada(), c.getPosicao(), c.getParticipanteA(), c.getParticipanteB(), c.getVencedor(), c.getPlacar());
    }
}
