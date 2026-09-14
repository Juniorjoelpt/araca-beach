package com.aracabeach.service;

import com.aracabeach.domain.despesa.Despesa;
import com.aracabeach.domain.despesa.DespesaRecorrente;
import com.aracabeach.dto.DespesaRecorrenteRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.DespesaRecorrenteRepository;
import com.aracabeach.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespesaRecorrenteService {

    private final DespesaRecorrenteRepository despesaRecorrenteRepository;
    private final DespesaRepository despesaRepository;

    @Transactional
    public DespesaRecorrente criar(DespesaRecorrenteRequest request) {
        DespesaRecorrente recorrente = DespesaRecorrente.builder()
                .descricao(request.descricao())
                .categoria(request.categoria())
                .valor(request.valor())
                .diaVencimento(request.diaVencimento())
                .ativa(true)
                .build();
        return despesaRecorrenteRepository.save(recorrente);
    }

    @Transactional(readOnly = true)
    public List<DespesaRecorrente> listarAtivas() {
        return despesaRecorrenteRepository.findByAtivaTrue();
    }

    @Transactional
    public void desativar(Long id) {
        DespesaRecorrente recorrente = despesaRecorrenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa recorrente não encontrada: " + id));
        recorrente.setAtiva(false);
        despesaRecorrenteRepository.save(recorrente);
    }

    /**
     * Para cada recorrencia ativa que ainda nao gerou a despesa do mes atual,
     * cria a Despesa correspondente e marca o mes como gerado. Idempotente:
     * rodar mais de uma vez no mesmo mes nao duplica lancamentos.
     */
    @Transactional
    public int gerarDespesasDoMes() {
        YearMonth mesAtual = YearMonth.now();
        String mesAtualStr = mesAtual.toString();
        int geradas = 0;

        for (DespesaRecorrente recorrente : despesaRecorrenteRepository.findByAtivaTrue()) {
            if (mesAtualStr.equals(recorrente.getUltimoMesGerado())) {
                continue;
            }

            int dia = Math.min(recorrente.getDiaVencimento(), mesAtual.lengthOfMonth());
            LocalDate vencimento = mesAtual.atDay(dia);

            Despesa despesa = Despesa.builder()
                    .descricao(recorrente.getDescricao())
                    .categoria(recorrente.getCategoria())
                    .valor(recorrente.getValor())
                    .dataVencimento(vencimento)
                    .paga(false)
                    .observacoes("Gerada automaticamente pela recorrência #" + recorrente.getId())
                    .build();
            despesaRepository.save(despesa);

            recorrente.setUltimoMesGerado(mesAtualStr);
            despesaRecorrenteRepository.save(recorrente);
            geradas++;
        }

        if (geradas > 0) {
            log.info("{} despesa(s) recorrente(s) geradas para o mês {}.", geradas, mesAtualStr);
        }
        return geradas;
    }
}
