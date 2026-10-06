package com.aracabeach.config;

import com.aracabeach.domain.comissao.OrigemComissao;
import com.aracabeach.domain.matricula.PagamentoMatricula;
import com.aracabeach.repository.AulaRepository;
import com.aracabeach.repository.PagamentoMatriculaRepository;
import com.aracabeach.service.ComissaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Ao subir, cria lancamentos de comissao para aulas avulsas e mensalidades de
 * turma pagas A PARTIR DO INICIO DO MES ATUAL que ainda nao tenham lancamento
 * (dados anteriores a esta versao). Meses anteriores nao sao importados para
 * nao reabrir como "pendente" comissoes que possivelmente ja foram acertadas
 * fora do sistema. Idempotente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComissaoBackfill implements ApplicationRunner {

    private final AulaRepository aulaRepository;
    private final PagamentoMatriculaRepository pagamentoMatriculaRepository;
    private final ComissaoService comissaoService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);

            aulaRepository.findByInicioBetween(
                    LocalDateTime.of(inicioMes, LocalTime.MIN), LocalDateTime.now().plusYears(1)).forEach(a ->
                    comissaoService.lancar(a.getProfessor(), OrigemComissao.AULA_AVULSA, a.getId(),
                            "Aula " + a.getTipo() + " - " + a.getInicio().toLocalDate(),
                            a.getInicio().toLocalDate(), a.getValor()));

            for (PagamentoMatricula p : pagamentoMatriculaRepository
                    .findByPagoTrueAndDataPagamentoGreaterThanEqual(inicioMes)) {
                comissaoService.lancar(p.getMatricula().getTurma().getProfessor(), OrigemComissao.MATRICULA,
                        p.getId(), "Mensalidade " + p.getMatricula().getTurma().getNome() + " " + p.getReferenciaMes(),
                        p.getDataPagamento(), p.getValor());
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel importar comissoes existentes: {}", e.getMessage());
        }
    }
}
