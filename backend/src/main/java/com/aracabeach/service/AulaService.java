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

        boolean porAluno = request.participantes() != null;
        Aula aula = Aula.builder()
                .professor(professor)
                .quadra(quadra)
                .tipo(request.tipo())
                .inicio(request.inicio())
                .fim(request.fim())
                .alunos(porAluno ? new java.util.ArrayList<>() : (request.alunos() != null ? request.alunos() : List.of()))
                .valor(porAluno ? java.math.BigDecimal.ZERO : totalDaAula(request))
                .valorPorAluno(request.valorPorAluno())
                .porAluno(porAluno)
                .build();

        Aula salva = aulaRepository.save(aula);
        if (porAluno) {
            for (var pr : request.participantes()) {
                incluirAluno(salva, pr);
            }
            recalcular(salva);
            return aulaRepository.save(salva);
        }
        comissaoService.lancar(professor, com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, salva.getId(),
                "Aula " + salva.getTipo() + " - " + salva.getInicio().toLocalDate(),
                salva.getInicio().toLocalDate(), salva.getValor(), horasDa(salva));
        if (Boolean.TRUE.equals(request.pago())) {
            salva = registrarPagamento(salva.getId(), request.formaPagamento());
        }
        return salva;
    }

    // ---------- Aula com alunos individuais ----------

    @Transactional
    public Aula adicionarAluno(Long aulaId, com.aracabeach.dto.AulaParticipanteRequest request) {
        Aula aula = buscar(aulaId);
        exigirPorAluno(aula);
        incluirAluno(aula, request);
        recalcular(aula);
        return aulaRepository.save(aula);
    }

    @Transactional
    public Aula pagarAluno(Long aulaId, Long alunoId, com.aracabeach.domain.financeiro.FormaPagamento forma) {
        Aula aula = buscar(aulaId);
        com.aracabeach.domain.aula.AulaParticipante p = participante(aula, alunoId);
        receber(aula, p, forma);
        return aulaRepository.save(aula);
    }

    @Transactional
    public Aula removerAluno(Long aulaId, Long alunoId) {
        Aula aula = buscar(aulaId);
        com.aracabeach.domain.aula.AulaParticipante p = participante(aula, alunoId);
        if (p.isPago()) {
            throw new IllegalArgumentException("Aluno ja pagou: nao pode ser removido (o valor ja entrou no caixa).");
        }
        comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, -p.getId());
        aula.getParticipantes().remove(p);
        recalcular(aula);
        return aulaRepository.save(aula);
    }

    private Aula buscar(Long id) {
        return aulaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula nao encontrada: " + id));
    }

    private void exigirPorAluno(Aula aula) {
        if (!aula.isPorAluno()) {
            throw new IllegalArgumentException("Esta aula foi criada no formato antigo (sem alunos individuais).");
        }
    }

    private com.aracabeach.domain.aula.AulaParticipante participante(Aula aula, Long alunoId) {
        return aula.getParticipantes().stream().filter(x -> x.getId().equals(alunoId)).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado nesta aula."));
    }

    private void incluirAluno(Aula aula, com.aracabeach.dto.AulaParticipanteRequest pr) {
        java.math.BigDecimal valor = pr.valor() != null ? pr.valor() : aula.getValorPorAluno();
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("Informe o valor do aluno (ou o valor padrao por aluno da aula).");
        }
        com.aracabeach.domain.aula.AulaParticipante p = com.aracabeach.domain.aula.AulaParticipante.builder()
                .aula(aula).nome(pr.nome().trim()).valor(valor.setScale(2, java.math.RoundingMode.HALF_UP)).build();
        aula.getParticipantes().add(p);
        aulaRepository.saveAndFlush(aula);
        if (Boolean.TRUE.equals(pr.pago())) {
            receber(aula, p, pr.formaPagamento());
        }
    }

    /** Registra o pagamento de um aluno e lanca no caixa. */
    private void receber(Aula aula, com.aracabeach.domain.aula.AulaParticipante p,
                         com.aracabeach.domain.financeiro.FormaPagamento forma) {
        if (p.isPago()) {
            throw new IllegalArgumentException("Esse aluno ja pagou.");
        }
        if (forma == null) {
            throw new IllegalArgumentException("Informe a forma de pagamento.");
        }
        if (p.getValor() == null || p.getValor().signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero para receber de " + p.getNome() + ".");
        }
        p.setPago(true);
        p.setDataPagamento(LocalDate.now());
        p.setFormaPagamento(forma);
        pagamentoRepository.save(com.aracabeach.domain.financeiro.Pagamento.builder()
                .reserva(null)
                .valor(p.getValor())
                .formaPagamento(forma)
                .status(com.aracabeach.domain.financeiro.StatusPagamento.PAGO)
                .ehSinal(false)
                .origem("MATRICULA")
                .descricao("Aula avulsa " + aula.getTipo() + " " + aula.getInicio().toLocalDate()
                        + " - " + p.getNome() + " (prof. " + aula.getProfessor().getNome() + ")")
                .operador(com.aracabeach.config.OperadorAtual.login())
                .build());
    }

    /** Atualiza total, nomes e comissao (por aluno para %, por hora da aula para professor por hora). */
    private void recalcular(Aula aula) {
        java.math.BigDecimal total = aula.getParticipantes().stream()
                .map(com.aracabeach.domain.aula.AulaParticipante::getValor)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        aula.setValor(total);
        aula.setAlunos(new java.util.ArrayList<>(aula.getParticipantes().stream()
                .map(com.aracabeach.domain.aula.AulaParticipante::getNome).toList()));
        Professor prof = aula.getProfessor();
        LocalDate dia = aula.getInicio().toLocalDate();
        if (prof.isPorHora()) {
            comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, aula.getId());
            comissaoService.lancar(prof, com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, aula.getId(),
                    "Aula " + aula.getTipo() + " - " + dia, dia, total, horasDa(aula));
        } else {
            for (var p : aula.getParticipantes()) {
                comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, -p.getId());
                comissaoService.lancar(prof, com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, -p.getId(),
                        "Aula " + aula.getTipo() + " - " + dia + " - " + p.getNome(), dia, p.getValor());
            }
        }
    }

    static java.math.BigDecimal horasDa(Aula a) {
        return java.math.BigDecimal.valueOf(java.time.Duration.between(a.getInicio(), a.getFim()).toMinutes())
                .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
    }

    private static java.math.BigDecimal totalDaAula(AulaRequest r) {
        if (r.valorPorAluno() == null) {
            return r.valor();
        }
        if (r.valorPorAluno().signum() < 0) {
            throw new IllegalArgumentException("O valor por aluno nao pode ser negativo.");
        }
        int alunos = r.alunos() == null ? 0 : (int) r.alunos().stream().filter(a -> a != null && !a.isBlank()).count();
        return r.valorPorAluno().multiply(java.math.BigDecimal.valueOf(Math.max(1, alunos)))
                .setScale(2, java.math.RoundingMode.HALF_UP);
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

        if (aula.isPorAluno()) {
            aula.setProfessor(professor);
            aula.setQuadra(quadra);
            aula.setTipo(request.tipo());
            aula.setInicio(request.inicio());
            aula.setFim(request.fim());
            if (request.valorPorAluno() != null) {
                aula.setValorPorAluno(request.valorPorAluno());
            }
            recalcular(aula);
            return aulaRepository.save(aula);
        }
        if (aula.isPago() && (totalDaAula(request) == null || totalDaAula(request).compareTo(aula.getValor()) != 0)) {
            throw new IllegalArgumentException("Aula ja paga: o valor nao pode ser alterado.");
        }
        aula.setProfessor(professor);
        aula.setQuadra(quadra);
        aula.setTipo(request.tipo());
        aula.setInicio(request.inicio());
        aula.setFim(request.fim());
        aula.setAlunos(new java.util.ArrayList<>(request.alunos() != null ? request.alunos() : List.of()));
        aula.setValor(totalDaAula(request));
        aula.setValorPorAluno(request.valorPorAluno());
        Aula salva = aulaRepository.save(aula);

        // Recalcula a comissao (so troca se ainda nao foi paga).
        comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, id);
        comissaoService.lancar(professor, com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, salva.getId(),
                "Aula " + salva.getTipo() + " - " + salva.getInicio().toLocalDate(),
                salva.getInicio().toLocalDate(), salva.getValor(), horasDa(salva));
        return salva;
    }

    @Transactional(readOnly = true)
    public List<Aula> listarPorDia(LocalDate data) {
        return aulaRepository.findByInicioBetween(LocalDateTime.of(data, LocalTime.MIN), LocalDateTime.of(data, LocalTime.MAX));
    }

    @Transactional
    public void remover(Long id) {
        Aula existente = aulaRepository.findById(id).orElse(null);
        if (existente != null && (existente.isPago()
                || existente.getParticipantes().stream().anyMatch(com.aracabeach.domain.aula.AulaParticipante::isPago))) {
            throw new IllegalArgumentException("Aula com pagamento recebido: nao pode ser removida (o valor ja entrou no caixa).");
        }
        if (existente != null) {
            existente.getParticipantes().forEach(x ->
                    comissaoService.estornar(com.aracabeach.domain.comissao.OrigemComissao.AULA_AVULSA, -x.getId()));
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

        java.math.BigDecimal horas = aulas.stream().map(AulaService::horasDa)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal percentual = professor.getPercentualComissao() != null
                ? professor.getPercentualComissao()
                : java.math.BigDecimal.ZERO;

        java.math.BigDecimal valorComissao;
        if (professor.isPorHora()) {
            java.math.BigDecimal vh = professor.getValorHora() != null ? professor.getValorHora() : java.math.BigDecimal.ZERO;
            valorComissao = horas.multiply(vh).setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            valorComissao = valorTotal
                    .multiply(percentual)
                    .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }

        return new com.aracabeach.dto.ComissaoProfessorResponse(
                professor.getId(), professor.getNome(), inicio, fim, aulas.size(), valorTotal, percentual, valorComissao,
                professor.isPorHora() ? "POR_HORA" : "PERCENTUAL", professor.getValorHora(), horas
        );
    }
}
