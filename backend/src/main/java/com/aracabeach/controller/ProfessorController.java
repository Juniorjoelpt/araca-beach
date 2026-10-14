package com.aracabeach.controller;

import com.aracabeach.domain.professor.Professor;
import com.aracabeach.dto.ProfessorRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.AulaPacoteRepository;
import com.aracabeach.repository.AulaRepository;
import com.aracabeach.repository.ComissaoLancamentoRepository;
import com.aracabeach.repository.ProfessorRepository;
import com.aracabeach.repository.TurmaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorRepository professorRepository;
    private final com.aracabeach.service.ModalidadeService modalidadeService;
    private final AulaRepository aulaRepository;
    private final TurmaRepository turmaRepository;
    private final AulaPacoteRepository aulaPacoteRepository;
    private final ComissaoLancamentoRepository comissaoRepository;

    @GetMapping
    public List<Professor> listar() {
        return professorRepository.findAll();
    }

    @PostMapping
    public Professor criar(@Valid @RequestBody ProfessorRequest request) {
        modalidadeService.exigir(request.especialidade());
        Professor professor = Professor.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .especialidade(request.especialidade())
                .percentualComissao(request.percentualComissao())
                .ativo(true)
                .build();
        return professorRepository.save(professor);
    }

    @PutMapping("/{id}")
    public Professor atualizar(@PathVariable Long id, @Valid @RequestBody ProfessorRequest request) {
        Professor professor = professorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + id));
        modalidadeService.exigir(request.especialidade());
        professor.setNome(request.nome());
        professor.setTelefone(request.telefone());
        professor.setEspecialidade(request.especialidade());
        professor.setPercentualComissao(request.percentualComissao());
        return professorRepository.save(professor);
    }

    /** So exclui professor sem nenhum vinculo (cadastro feito por engano). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        Professor professor = professorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + id));
        if (aulaRepository.existsByProfessorId(id) || turmaRepository.existsByProfessorId(id)
                || aulaPacoteRepository.existsByProfessorId(id) || comissaoRepository.existsByProfessorId(id)) {
            throw new IllegalArgumentException("Este professor tem aulas, turmas ou comissões vinculadas e não pode ser excluído. Edite os dados dele se houver erro.");
        }
        professorRepository.delete(professor);
    }
}
