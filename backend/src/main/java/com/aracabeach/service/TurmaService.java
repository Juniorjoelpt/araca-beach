package com.aracabeach.service;

import com.aracabeach.domain.professor.Professor;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.turma.Turma;
import com.aracabeach.dto.TurmaRequest;
import com.aracabeach.dto.TurmaResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.MatriculaRepository;
import com.aracabeach.repository.ProfessorRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.TurmaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Turma e a aula em grupo recorrente (ver Turma.java). Os alunos entram
 * atraves de MatriculaService, que tambem cuida da cobranca mensal.
 */
@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final ProfessorRepository professorRepository;
    private final QuadraRepository quadraRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional
    public TurmaResponse criar(TurmaRequest request) {
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }

        Professor professor = professorRepository.findById(request.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + request.professorId()));
        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));

        Turma turma = Turma.builder()
                .nome(request.nome())
                .tipo(request.tipo())
                .professor(professor)
                .quadra(quadra)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .capacidadeMaxima(request.capacidadeMaxima())
                .ativa(true)
                .build();

        return paraResponse(turmaRepository.save(turma));
    }

    @Transactional(readOnly = true)
    public List<TurmaResponse> listarAtivas() {
        return turmaRepository.findByAtivaTrue().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Turma buscarPorId(Long id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma nao encontrada: " + id));
    }

    @Transactional
    public void desativar(Long id) {
        Turma turma = buscarPorId(id);
        turma.setAtiva(false);
        turmaRepository.save(turma);
    }

    private TurmaResponse paraResponse(Turma t) {
        int matriculasAtivas = matriculaRepository.findByTurmaIdAndAtivaTrue(t.getId()).size();
        return new TurmaResponse(
                t.getId(),
                t.getNome(),
                t.getTipo(),
                t.getProfessor().getId(),
                t.getProfessor().getNome(),
                t.getQuadra().getId(),
                t.getQuadra().getNome(),
                t.getDiaSemana().name(),
                t.getHoraInicio().toString(),
                t.getHoraFim().toString(),
                t.getCapacidadeMaxima(),
                matriculasAtivas,
                t.isAtiva()
        );
    }
}
