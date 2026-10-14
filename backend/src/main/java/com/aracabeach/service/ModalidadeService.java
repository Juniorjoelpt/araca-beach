package com.aracabeach.service;

import com.aracabeach.domain.quadra.Modalidade;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModalidadeService {

    private static final int MAX_CODIGO = 20;

    private final ModalidadeRepository modalidadeRepository;
    private final QuadraRepository quadraRepository;
    private final ProfessorRepository professorRepository;
    private final TorneioRepository torneioRepository;
    private final TurmaRepository turmaRepository;
    private final PlanoPacoteRepository planoPacoteRepository;

    @Transactional(readOnly = true)
    public List<Modalidade> listar() {
        return modalidadeRepository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Modalidade criar(String nomeInformado) {
        String nome = limpar(nomeInformado);
        if (modalidadeRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException("Já existe uma modalidade com este nome.");
        }
        return modalidadeRepository.save(Modalidade.builder().codigo(codigoUnico(nome)).nome(nome).build());
    }

    /** Renomeia so o nome exibido; o codigo (usado nos cadastros) nao muda. */
    @Transactional
    public Modalidade renomear(Long id, String nomeInformado) {
        Modalidade m = modalidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade nao encontrada: " + id));
        String nome = limpar(nomeInformado);
        if (modalidadeRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new IllegalArgumentException("Já existe uma modalidade com este nome.");
        }
        m.setNome(nome);
        return modalidadeRepository.save(m);
    }

    @Transactional
    public void excluir(Long id) {
        Modalidade m = modalidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade nao encontrada: " + id));
        String c = m.getCodigo();
        if (quadraRepository.existsByTipo(c) || professorRepository.existsByEspecialidade(c)
                || torneioRepository.existsByModalidade(c) || turmaRepository.existsByTipo(c)
                || planoPacoteRepository.existsByTipo(c)) {
            throw new IllegalArgumentException("Esta modalidade está em uso (quadras, professores, turmas, torneios ou pacotes) e não pode ser removida.");
        }
        modalidadeRepository.delete(m);
    }

    /** Garante que o codigo informado existe; usado ao salvar quadra, professor e torneio. */
    @Transactional(readOnly = true)
    public void exigir(String codigo) {
        if (codigo == null || codigo.isBlank() || !modalidadeRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Modalidade inválida. Cadastre-a antes em Quadras → Modalidades.");
        }
    }

    private String limpar(String nome) {
        String n = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new IllegalArgumentException("Informe o nome da modalidade.");
        return n;
    }

    /** "Beach Tênis Kids" -> BEACH_TENIS_KIDS (maximo 20 caracteres); acrescenta _2, _3... se ja existir. */
    String codigoUnico(String nome) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "MODALIDADE";
        if (base.length() > MAX_CODIGO) base = base.substring(0, MAX_CODIGO).replaceAll("_+$", "");
        String codigo = base;
        int n = 2;
        while (modalidadeRepository.existsByCodigo(codigo)) {
            String sufixo = "_" + n++;
            codigo = base.substring(0, Math.min(base.length(), MAX_CODIGO - sufixo.length())) + sufixo;
        }
        return codigo;
    }
}
