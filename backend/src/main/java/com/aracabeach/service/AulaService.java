package com.aracabeach.service;

import com.aracabeach.domain.aula.Aula;
import com.aracabeach.domain.professor.Professor;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.dto.AulaRequest;
import com.aracabeach.exception.ConflitoHorarioException;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.AulaRepository;
import com.aracabeach.repository.ProfessorRepository;
import com.aracabeach.repository.QuadraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository aulaRepository;
    private final ProfessorRepository professorRepository;
    private final QuadraRepository quadraRepository;

    @Transactional
    public Aula criar(AulaRequest request) {
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }

        Professor professor = professorRepository.findById(request.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + request.professorId()));

        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));

        boolean conflito = !aulaRepository
                .findConflitantesDoProfessor(professor.getId(), request.inicio(), request.fim())
                .isEmpty();
        if (conflito) {
            throw new ConflitoHorarioException("O professor ja tem uma aula nesse horario.");
        }

        Aula aula = Aula.builder()
                .professor(professor)
                .quadra(quadra)
                .tipo(request.tipo())
                .inicio(request.inicio())
                .fim(request.fim())
                .alunos(request.alunos() != null ? request.alunos() : List.of())
                .valor(request.valor())
                .build();

        return aulaRepository.save(aula);
    }

    @Transactional(readOnly = true)
    public List<Aula> listarPorDia(LocalDate data) {
        return aulaRepository.findByInicioBetween(LocalDateTime.of(data, LocalTime.MIN), LocalDateTime.of(data, LocalTime.MAX));
    }

    @Transactional
    public void remover(Long id) {
        aulaRepository.deleteById(id);
    }

    /**
     * Calcula a comissao de um professor num periodo, com base no valor
     * lancado em cada aula (campo opcional) e no percentual de comissao
     * cadastrado no professor. Aulas sem valor informado contam como R$ 0.
     */
    @Transactional(readOnly = true)
    public com.aracabeach.dto.ComissaoProfessorResponse calcularComissao(Long professorId, java.time.LocalDate inicio, java.time.LocalDate fim) {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + professorId));

        LocalDateTime inicioDateTime = LocalDateTime.of(inicio, LocalTime.MIN);
        LocalDateTime fimDateTime = LocalDateTime.of(fim, LocalTime.MAX);

        List<Aula> aulas = aulaRepository.findByProfessorIdAndInicioBetween(professorId, inicioDateTime, fimDateTime);

        java.math.BigDecimal valorTotal = aulas.stream()
                .map(Aula::getValor)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal percentual = professor.getPercentualComissao() != null
                ? professor.getPercentualComissao()
                : java.math.BigDecimal.ZERO;

        java.math.BigDecimal valorComissao = valorTotal
                .multiply(percentual)
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        return new com.aracabeach.dto.ComissaoProfessorResponse(
                professor.getId(), professor.getNome(), inicio, fim, aulas.size(), valorTotal, percentual, valorComissao
        );
    }
}
