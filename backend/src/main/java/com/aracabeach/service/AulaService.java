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
    private final ComissaoService comissaoService;
    private final com.aracabeach.repository.PagamentoRepository pagamentoRepository;

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

        Aula salva = aulaRepository.save(aula);
        comissaoService.lancar(professor, com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, salva.getId(),
                "Aula " + salva.getTipo() + " - " + salva.getInicio().toLocalDate(),
                salva.getInicio().toLocalDate(), salva.getValor());
        if (Boolean.TRUE.equals(request.pago())) {
            salva = registrarPagamento(salva.getId(), request.formaPagamento());
        }
        return salva;
    }

    /** Recebe a aula avulsa: marca como paga e lanca o valor no caixa. */
    @Transactional
    public Aula registrarPagamento(Long id, com.aracabeach.domain.financeiro.FormaPagamento forma) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula nao encontrada: " + id));
        if (aula.isPago()) {
            throw new IllegalArgumentException("Essa aula ja foi paga.");
        }
        if (aula.getValor() == null || aula.getValor().signum() <= 0) {
            throw new IllegalArgumentException("Informe o valor da aula antes de registrar o pagamento.");
        }
        if (forma == null) {
            throw new IllegalArgumentException("Informe a forma de pagamento.");
        }
        aula.setPago(true);
        aula.setDataPagamento(LocalDate.now());
        aula.setFormaPagamento(forma);
        aula = aulaRepository.save(aula);

        pagamentoRepository.save(com.aracabeach.domain.financeiro.Pagamento.builder()
                .reserva(null)
                .valor(aula.getValor())
                .formaPagamento(forma)
                .status(com.aracabeach.domain.financeiro.StatusPagamento.PAGO)
                .ehSinal(false)
                .origem("MATRICULA")
                .descricao("Aula avulsa " + aula.getTipo() + " " + aula.getInicio().toLocalDate()
                        + " - " + aula.getProfessor().getNome())
                .operador(com.aracabeach.config.OperadorAtual.login())
                .build());
        return aula;
    }

    @Transactional
    public Aula atualizar(Long id, AulaRequest request) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula nao encontrada: " + id));
        if (!request.fim().isAfter(request.inicio())) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }
        Professor professor = professorRepository.findById(request.professorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado: " + request.professorId()));
        Quadra quadra = quadraRepository.findById(request.quadraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadra nao encontrada: " + request.quadraId()));

        boolean conflito = aulaRepository.findConflitantesDoProfessor(professor.getId(), request.inicio(), request.fim())
                .stream().anyMatch(a -> !a.getId().equals(id));
        if (conflito) {
            throw new ConflitoHorarioException("O professor ja tem uma aula nesse horario.");
        }

        if (aula.isPago() && (request.valor() == null || request.valor().compareTo(aula.getValor()) != 0)) {
            throw new IllegalArgumentException("Aula ja paga: o valor nao pode ser alterado.");
        }
        aula.setProfessor(professor);
        aula.setQuadra(quadra);
        aula.setTipo(request.tipo());
        aula.setInicio(request.inicio());
        aula.setFim(request.fim());
        aula.setAlunos(new java.util.ArrayList<>(request.alunos() != null ? request.alunos() : List.of()));
        aula.setValor(request.valor());
        Aula salva = aulaRepository.save(aula);

        // Recalcula a comissao (so troca se ainda nao foi paga).
        comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, id);
        comissaoService.lancar(professor, com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, salva.getId(),
                "Aula " + salva.getTipo() + " - " + salva.getInicio().toLocalDate(),
                salva.getInicio().toLocalDate(), salva.getValor());
        return salva;
    }

    @Transactional(readOnly = true)
    public List<Aula> listarPorDia(LocalDate data) {
        return aulaRepository.findByInicioBetween(LocalDateTime.of(data, LocalTime.MIN), LocalDateTime.of(data, LocalTime.MAX));
    }

    @Transactional
    public void remover(Long id) {
        if (aulaRepository.findById(id).map(Aula::isPago).orElse(false)) {
            throw new IllegalArgumentException("Aula ja paga: nao pode ser removida (o valor ja entrou no caixa).");
        }
        comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, id);
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
