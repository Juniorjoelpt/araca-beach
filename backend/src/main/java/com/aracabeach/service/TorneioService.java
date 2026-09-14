package com.aracabeach.service;

import com.aracabeach.domain.torneio.Inscricao;
import com.aracabeach.domain.torneio.Torneio;
import com.aracabeach.dto.InscricaoRequest;
import com.aracabeach.dto.TorneioRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.InscricaoRepository;
import com.aracabeach.repository.TorneioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TorneioService {

    private final TorneioRepository torneioRepository;
    private final InscricaoRepository inscricaoRepository;

    @Transactional
    public Torneio criar(TorneioRequest request) {
        Torneio torneio = Torneio.builder()
                .nome(request.nome())
                .modalidade(request.modalidade())
                .dataInicio(request.dataInicio())
                .dataFim(request.dataFim())
                .categorias(request.categorias() != null ? request.categorias() : List.of())
                .build();
        return torneioRepository.save(torneio);
    }

    @Transactional(readOnly = true)
    public List<Torneio> listarTodos() {
        return torneioRepository.findAll();
    }

    @Transactional
    public Inscricao inscrever(InscricaoRequest request) {
        Torneio torneio = torneioRepository.findById(request.torneioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Torneio nao encontrado: " + request.torneioId()));

        Inscricao inscricao = Inscricao.builder()
                .torneio(torneio)
                .participante(request.participante())
                .parceiro(request.parceiro())
                .categoria(request.categoria())
                .build();

        return inscricaoRepository.save(inscricao);
    }

    @Transactional(readOnly = true)
    public List<Inscricao> listarInscricoes(Long torneioId) {
        return inscricaoRepository.findByTorneioId(torneioId);
    }
}
