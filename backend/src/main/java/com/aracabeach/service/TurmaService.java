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
    private final com.aracabeach.repository.BloqueioQuadraRepository bloqueioRepository;

    private static final int HORIZONTE_SEMANAS = 12;

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

        turma = turmaRepository.save(turma);
        sincronizarBloqueios(turma);
        return paraResponse(turma);
    }

    @Transactional
    public TurmaResponse atualizar(Long id, TurmaRequest request) {
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }
        Turma turma = buscarPorId(id);
        Professor professor = professorRepository.findById(request.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + request.professorId()));
        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));
        turma.setNome(request.nome());
        turma.setTipo(request.tipo());
        turma.setProfessor(professor);
        turma.setQuadra(quadra);
        turma.setDiaSemana(request.diaSemana());
        turma.setHoraInicio(request.horaInicio());
        turma.setHoraFim(request.horaFim());
        turma.setCapacidadeMaxima(request.capacidadeMaxima());
        turma = turmaRepository.save(turma);
        sincronizarBloqueios(turma);
        return paraResponse(turma);
    }

    /** Exclui a turma so se nunca teve matriculas (cadastro feito por engano). Senao, use desativar. */
    @Transactional
    public void excluir(Long id) {
        Turma turma = buscarPorId(id);
        if (matriculaRepository.existsByTurmaId(id)) {
            throw new IllegalArgumentException("Esta turma tem matrículas (ativas ou antigas) e não pode ser excluída. Use Desativar para tirá-la da lista.");
        }
        bloqueioRepository.deleteByTurmaId(id);
        turmaRepository.delete(turma);
    }

    /**
     * Faz a turma "ocupar" a quadra: gera um bloqueio de agenda para cada ocorrencia semanal
     * nas proximas semanas. Assim a recepcao, o portal, as recorrencias e os pacotes
     * enxergam o horario como indisponivel. Apaga e recria as ocorrencias futuras.
     */
    @Transactional
    public void sincronizarBloqueios(Turma turma) {
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        bloqueioRepository.deleteByTurmaIdAndFimAfter(turma.getId(), agora);
        if (!turma.isAtiva()) return;

        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate limite = hoje.plusWeeks(HORIZONTE_SEMANAS);
        java.time.LocalDate data = hoje.with(java.time.temporal.TemporalAdjusters.nextOrSame(turma.getDiaSemana()));
        for (; !data.isAfter(limite); data = data.plusWeeks(1)) {
            java.time.LocalDateTime inicio = java.time.LocalDateTime.of(data, turma.getHoraInicio());
            java.time.LocalDateTime fim = java.time.LocalDateTime.of(data, turma.getHoraFim());
            if (!fim.isAfter(agora)) continue;
            bloqueioRepository.save(com.aracabeach.domain.regra.BloqueioQuadra.builder()
                    .quadra(turma.getQuadra())
                    .inicio(inicio)
                    .fim(fim)
                    .motivo("Turma: " + turma.getNome())
                    .turmaId(turma.getId())
                    .build());
        }
    }

    @Transactional
    public void sincronizarTodas() {
        turmaRepository.findByAtivaTrue().forEach(this::sincronizarBloqueios);
    }

    /** Ocorrencias das turmas ativas de uma quadra no periodo, para mostrar no calendario. */
    @Transactional(readOnly = true)
    public List<com.aracabeach.dto.TurmaAgendaResponse> agenda(Long quadraId, java.time.LocalDate de, java.time.LocalDate ate) {
        List<com.aracabeach.dto.TurmaAgendaResponse> lista = new java.util.ArrayList<>();
        for (Turma t : turmaRepository.findByAtivaTrue()) {
            if (!t.getQuadra().getId().equals(quadraId)) continue;
            java.time.LocalDate data = de.with(java.time.temporal.TemporalAdjusters.nextOrSame(t.getDiaSemana()));
            for (; !data.isAfter(ate); data = data.plusWeeks(1)) {
                lista.add(new com.aracabeach.dto.TurmaAgendaResponse(t.getId(), t.getNome(), t.getProfessor().getNome(),
                        java.time.LocalDateTime.of(data, t.getHoraInicio()), java.time.LocalDateTime.of(data, t.getHoraFim()),
                        matriculaRepository.findByTurmaIdAndAtivaTrue(t.getId()).size()));
            }
        }
        return lista;
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
        bloqueioRepository.deleteByTurmaIdAndFimAfter(id, java.time.LocalDateTime.now());
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
