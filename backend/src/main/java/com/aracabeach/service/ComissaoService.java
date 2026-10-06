package com.aracabeach.service;

import com.aracabeach.domain.comissao.ComissaoLancamento;
import com.aracabeach.domain.comissao.OrigemComissao;
import com.aracabeach.domain.comissao.StatusComissao;
import com.aracabeach.domain.despesa.CategoriaDespesa;
import com.aracabeach.domain.despesa.Despesa;
import com.aracabeach.domain.professor.Professor;
import com.aracabeach.dto.ComissaoLancamentoResponse;
import com.aracabeach.dto.ComissaoResumoProfessorResponse;
import com.aracabeach.dto.PagarComissoesRequest;
import com.aracabeach.repository.ComissaoLancamentoRepository;
import com.aracabeach.repository.DespesaRepository;
import com.aracabeach.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Livro de comissoes dos professores. Os lancamentos sao criados
 * automaticamente (aula avulsa lancada, mensalidade de turma paga, aula de
 * pacote realizada) e sao idempotentes por (origem, referencia): lancar duas
 * vezes o mesmo fato nao duplica a comissao.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComissaoService {

    private final ComissaoLancamentoRepository repository;
    private final ProfessorRepository professorRepository;
    private final DespesaRepository despesaRepository;

    /** Cria o lancamento se ainda nao existir e se o professor tiver percentual > 0. */
    @Transactional
    public void lancar(Professor professor, OrigemComissao origem, Long referenciaId,
                       String descricao, LocalDate competencia, BigDecimal base) {
        if (professor == null || base == null || base.signum() <= 0) {
            return;
        }
        BigDecimal percentual = professor.getPercentualComissao();
        if (percentual == null || percentual.signum() <= 0) {
            return;
        }
        if (repository.findByOrigemAndReferenciaId(origem, referenciaId).isPresent()) {
            return;
        }
        BigDecimal valor = base.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        repository.save(ComissaoLancamento.builder()
                .professor(professor)
                .origem(origem)
                .referenciaId(referenciaId)
                .descricao(descricao)
                .competencia(competencia)
                .base(base)
                .percentual(percentual)
                .valor(valor)
                .build());
    }

    /** Remove o lancamento se ainda nao foi pago (ex.: aula avulsa excluida). */
    @Transactional
    public void estornar(OrigemComissao origem, Long referenciaId) {
        repository.findByOrigemAndReferenciaId(origem, referenciaId)
                .filter(c -> c.getStatus() == StatusComissao.PENDENTE)
                .ifPresent(repository::delete);
    }

    @Transactional(readOnly = true)
    public List<ComissaoLancamentoResponse> listar(Long professorId, StatusComissao status,
                                                   LocalDate inicio, LocalDate fim) {
        return repository.buscar(professorId, status, inicio, fim).stream().map(this::paraResponse).toList();
    }

    /** Resumo por professor (pendente + pago) no periodo. */
    @Transactional(readOnly = true)
    public List<ComissaoResumoProfessorResponse> resumo(LocalDate inicio, LocalDate fim) {
        List<ComissaoLancamento> todos = repository.buscar(null, null, inicio, fim);
        return professorRepository.findAll().stream().map(p -> {
            List<ComissaoLancamento> doProf = todos.stream()
                    .filter(c -> c.getProfessor().getId().equals(p.getId())).toList();
            List<ComissaoLancamento> pend = doProf.stream()
                    .filter(c -> c.getStatus() == StatusComissao.PENDENTE).toList();
            BigDecimal valorPend = soma(pend);
            BigDecimal valorPago = soma(doProf.stream().filter(c -> c.getStatus() == StatusComissao.PAGA).toList());
            return new ComissaoResumoProfessorResponse(p.getId(), p.getNome(),
                    p.getPercentualComissao() != null ? p.getPercentualComissao() : BigDecimal.ZERO,
                    pend.size(), valorPend, valorPago);
        }).filter(r -> r.quantidadePendentes() > 0 || r.valorPago().signum() > 0).toList();
    }

    @Transactional
    public List<ComissaoLancamentoResponse> pagar(PagarComissoesRequest request) {
        List<Long> idsUnicos = request.ids().stream().distinct().toList();
        List<ComissaoLancamento> lancamentos = repository.findAllById(idsUnicos);
        if (lancamentos.size() != idsUnicos.size()) {
            throw new IllegalArgumentException("Algum lancamento informado nao foi encontrado.");
        }
        for (ComissaoLancamento c : lancamentos) {
            if (!c.getProfessor().getId().equals(request.professorId())) {
                throw new IllegalArgumentException("Os lancamentos devem ser do mesmo professor.");
            }
            if (c.getStatus() == StatusComissao.PAGA) {
                throw new IllegalArgumentException("O lancamento " + c.getId() + " ja foi pago.");
            }
        }
        LocalDate hoje = LocalDate.now();
        lancamentos.forEach(c -> {
            c.setStatus(StatusComissao.PAGA);
            c.setDataPagamento(hoje);
        });
        repository.saveAll(lancamentos);

        if (request.lancarDespesa()) {
            Professor professor = lancamentos.get(0).getProfessor();
            despesaRepository.save(Despesa.builder()
                    .descricao("Comissao - " + professor.getNome() + " (" + lancamentos.size() + " lancamento(s))")
                    .categoria(CategoriaDespesa.COMISSAO)
                    .valor(soma(lancamentos))
                    .dataVencimento(hoje)
                    .dataPagamento(hoje)
                    .paga(true)
                    .build());
        }
        return lancamentos.stream().map(this::paraResponse).toList();
    }

    private BigDecimal soma(List<ComissaoLancamento> itens) {
        return itens.stream().map(ComissaoLancamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ComissaoLancamentoResponse paraResponse(ComissaoLancamento c) {
        return new ComissaoLancamentoResponse(c.getId(), c.getProfessor().getId(), c.getProfessor().getNome(),
                c.getOrigem().name(), c.getDescricao(), c.getCompetencia(), c.getBase(), c.getPercentual(),
                c.getValor(), c.getStatus().name(), c.getDataPagamento());
    }
}
