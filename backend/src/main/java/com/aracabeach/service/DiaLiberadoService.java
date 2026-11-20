package com.aracabeach.service;

import com.aracabeach.domain.aula.Aula;
import com.aracabeach.domain.regra.BloqueioQuadra;
import com.aracabeach.domain.regra.DiaLiberado;
import com.aracabeach.domain.reserva.OrigemReserva;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.dto.DiaLiberadoPreviaResponse;
import com.aracabeach.dto.DiaLiberadoPreviaResponse.Item;
import com.aracabeach.dto.DiaLiberadoResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.AulaRepository;
import com.aracabeach.repository.BloqueioQuadraRepository;
import com.aracabeach.repository.DiaLiberadoRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dia liberado (feriado): cancela so naquele dia as reservas fixas (recorrentes e de
 * mensalidade combinada), libera as turmas e cancela as aulas avulsas, deixando a quadra livre
 * para reserva normal. Reservas ja pagas e aulas com pagamento nao sao canceladas: viram aviso.
 * Mensalidades continuam com o valor fixo (nada e cobrado ou descontado por aqui).
 */
@Service
@RequiredArgsConstructor
public class DiaLiberadoService {

    private static final String MARCA = "Dia liberado #";

    private final DiaLiberadoRepository diaLiberadoRepository;
    private final ReservaRepository reservaRepository;
    private final BloqueioQuadraRepository bloqueioRepository;
    private final AulaRepository aulaRepository;
    private final AulaService aulaService;
    private final PagamentoRepository pagamentoRepository;
    private final TurmaService turmaService;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<DiaLiberadoResponse> listar() {
        return diaLiberadoRepository.findByDataGreaterThanEqualOrderByDataAsc(LocalDate.now().minusDays(30)).stream()
                .map(d -> new DiaLiberadoResponse(d.getId(), d.getData(), d.getMotivo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaLiberadoPreviaResponse previa(LocalDate data) {
        return montar(data, false);
    }

    @Transactional
    public DiaLiberadoResponse liberar(LocalDate data, String motivo) {
        if (diaLiberadoRepository.existsByData(data)) {
            throw new IllegalArgumentException("Esse dia ja esta liberado.");
        }
        DiaLiberado dia = diaLiberadoRepository.save(DiaLiberado.builder()
                .data(data).motivo(motivo != null && !motivo.isBlank() ? motivo.trim() : "Feriado").build());

        DiaLiberadoPreviaResponse r = montar(data, true);
        // marca e cancela as reservas fixas sem pagamento
        for (Reserva res : reservasFixasDoDia(data)) {
            if (temPagamento(res)) continue;
            res.setStatus(StatusReserva.CANCELADA);
            res.setCanceladaEm(LocalDateTime.now());
            res.setTaxaCancelamento(BigDecimal.ZERO);
            res.setObservacoes(anexar(res.getObservacoes(), MARCA + dia.getId() + " (" + dia.getMotivo() + ")"));
            reservaRepository.save(res);
        }
        // libera as turmas naquele dia
        bloqueioRepository.deleteAll(bloqueiosDeTurma(data));
        // cancela as aulas avulsas sem pagamento
        for (Aula a : aulasDoDia(data)) {
            if (!aulaTemPagamento(a)) {
                aulaService.remover(a.getId());
            }
        }
        auditoria.detalhe("Dia liberado " + data + " (" + dia.getMotivo() + "): "
                + r.reservasCanceladas().size() + " reservas, " + r.turmasLiberadas().size() + " turmas, "
                + r.aulasCanceladas().size() + " aulas; " + r.avisos().size() + " avisos");
        return new DiaLiberadoResponse(dia.getId(), dia.getData(), dia.getMotivo());
    }

    /** Remove a liberacao: volta as turmas e restaura as reservas fixas que nao tenham conflito. */
    @Transactional
    public void desfazer(Long id) {
        DiaLiberado dia = diaLiberadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dia liberado nao encontrado: " + id));
        diaLiberadoRepository.delete(dia);
        diaLiberadoRepository.flush();

        LocalDate data = dia.getData();
        if (!data.isBefore(LocalDate.now())) {
            String marca = MARCA + id + " ";
            for (Reserva res : reservaRepository.findByInicioBetween(data.atStartOfDay(), data.plusDays(1).atStartOfDay())) {
                if (res.getStatus() == StatusReserva.CANCELADA && res.getObservacoes() != null
                        && res.getObservacoes().contains(marca)
                        && reservaRepository.findConflitantes(res.getQuadra().getId(), res.getInicio(), res.getFim()).isEmpty()
                        && bloqueioRepository.findConflitantes(res.getQuadra().getId(), res.getInicio(), res.getFim()).isEmpty()) {
                    res.setStatus(StatusReserva.CONFIRMADA);
                    res.setCanceladaEm(null);
                    res.setObservacoes(res.getObservacoes().replace(" | " + marca, "").replace(marca, "").trim());
                    reservaRepository.save(res);
                }
            }
            turmaService.sincronizarTodas();
        }
        auditoria.detalhe("Liberacao do dia " + data + " desfeita");
    }

    // ---- apoio ----

    private DiaLiberadoPreviaResponse montar(LocalDate data, boolean aposLiberar) {
        List<Item> reservas = new ArrayList<>();
        List<Item> turmas = new ArrayList<>();
        List<Item> aulas = new ArrayList<>();
        List<Item> avisos = new ArrayList<>();

        for (Reserva r : reservasFixasDoDia(data)) {
            Item item = new Item(r.getId(), hora(r.getInicio(), r.getFim()), r.getQuadra().getNome(),
                    r.getCliente() != null ? r.getCliente().getNome() : "");
            if (temPagamento(r)) {
                avisos.add(new Item(r.getId(), item.hora(), item.quadra(),
                        "Reserva ja paga (" + item.descricao() + "): nao foi cancelada, resolva manualmente"));
            } else {
                reservas.add(item);
            }
        }
        // aulas de pacote tambem ocupam a quadra: so avisar
        for (Reserva r : reservaRepository.findByInicioBetween(data.atStartOfDay(), data.plusDays(1).atStartOfDay())) {
            if (r.getStatus() == StatusReserva.CONFIRMADA && r.getOrigem() == OrigemReserva.AULA) {
                avisos.add(new Item(r.getId(), hora(r.getInicio(), r.getFim()), r.getQuadra().getNome(),
                        "Aula de pacote nesse dia: remarque pela tela de Pacotes se precisar"));
            }
        }
        for (BloqueioQuadra b : bloqueiosDeTurma(data)) {
            turmas.add(new Item(b.getId(), hora(b.getInicio(), b.getFim()),
                    b.getQuadra() != null ? b.getQuadra().getNome() : "Todas", b.getMotivo()));
        }
        for (Aula a : aulasDoDia(data)) {
            Item item = new Item(a.getId(), hora(a.getInicio(), a.getFim()), a.getQuadra().getNome(),
                    "Aula " + a.getTipo() + " - prof. " + a.getProfessor().getNome());
            if (aulaTemPagamento(a)) {
                avisos.add(new Item(a.getId(), item.hora(), item.quadra(),
                        item.descricao() + ": ja tem pagamento recebido, nao foi cancelada (use o estorno em Aulas)"));
            } else {
                aulas.add(item);
            }
        }
        return new DiaLiberadoPreviaResponse(data, aposLiberar || diaLiberadoRepository.existsByData(data),
                reservas, turmas, aulas, avisos);
    }

    /** Reservas confirmadas do dia que vem de recorrencia ou de mensalidade combinada (matricula). */
    private List<Reserva> reservasFixasDoDia(LocalDate data) {
        return reservaRepository.findByInicioBetween(data.atStartOfDay(), data.plusDays(1).atStartOfDay()).stream()
                .filter(r -> r.getStatus() == StatusReserva.CONFIRMADA)
                .filter(r -> r.getReservaRecorrenteId() != null
                        || (r.getObservacoes() != null && r.getObservacoes().startsWith("Matr")
                            && r.getObservacoes().contains("mensalidade combinada")))
                .toList();
    }

    private List<BloqueioQuadra> bloqueiosDeTurma(LocalDate data) {
        return bloqueioRepository.findByTurmaIdIsNotNullAndInicioGreaterThanEqualAndInicioLessThan(
                data.atStartOfDay(), data.plusDays(1).atStartOfDay());
    }

    private List<Aula> aulasDoDia(LocalDate data) {
        return aulaRepository.findByInicioBetween(data.atStartOfDay(), data.plusDays(1).atStartOfDay());
    }

    private boolean temPagamento(Reserva r) {
        return pagamentoRepository.findByReservaId(r.getId()).stream()
                .anyMatch(p -> p.getStatus() == StatusPagamento.PAGO);
    }

    private boolean aulaTemPagamento(Aula a) {
        return a.isPago() || a.getParticipantes().stream().anyMatch(com.aracabeach.domain.aula.AulaParticipante::isPago);
    }

    private static String hora(LocalDateTime ini, LocalDateTime fim) {
        return ini.toLocalTime().toString().substring(0, 5) + "-" + fim.toLocalTime().toString().substring(0, 5);
    }

    private static String anexar(String atual, String extra) {
        String base = atual == null ? "" : atual;
        String novo = base.isEmpty() ? extra : base + " | " + extra;
        if (novo.length() <= 300) return novo;
        int sobra = 300 - extra.length() - 3;
        return (sobra > 0 ? base.substring(0, Math.min(base.length(), sobra)) + " | " : "") + extra;
    }
}
