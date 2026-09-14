package com.aracabeach.service;

import com.aracabeach.domain.despesa.Despesa;
import com.aracabeach.dto.DespesaRequest;
import com.aracabeach.dto.ResumoDespesasResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;

    @Transactional
    public Despesa criar(DespesaRequest request) {
        Despesa despesa = Despesa.builder()
                .descricao(request.descricao())
                .categoria(request.categoria())
                .valor(request.valor())
                .dataVencimento(request.dataVencimento())
                .paga(false)
                .observacoes(request.observacoes())
                .build();
        return despesaRepository.save(despesa);
    }

    @Transactional(readOnly = true)
    public List<Despesa> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return despesaRepository.findByDataVencimentoBetweenOrderByDataVencimento(inicio, fim);
    }

    @Transactional
    public Despesa marcarComoPaga(Long id) {
        Despesa despesa = buscarPorId(id);
        despesa.setPaga(true);
        despesa.setDataPagamento(LocalDate.now());
        return despesaRepository.save(despesa);
    }

    @Transactional
    public void remover(Long id) {
        despesaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ResumoDespesasResponse resumo(LocalDate inicio, LocalDate fim) {
        List<Despesa> despesas = listarPorPeriodo(inicio, fim);

        BigDecimal totalPago = despesas.stream()
                .filter(Despesa::isPaga)
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPendente = despesas.stream()
                .filter(d -> !d.isPaga())
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumoDespesasResponse(inicio, fim, totalPago, totalPendente, totalPago.add(totalPendente));
    }

    private Despesa buscarPorId(Long id) {
        return despesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada: " + id));
    }
}
