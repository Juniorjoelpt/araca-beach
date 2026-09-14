package com.aracabeach.service;

import com.aracabeach.domain.aula.Aula;
import com.aracabeach.domain.produto.Comanda;
import com.aracabeach.domain.produto.ItemComanda;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.AulaResumoResponse;
import com.aracabeach.dto.DashboardResponse;
import com.aracabeach.dto.EstatisticaDiaResponse;
import com.aracabeach.dto.OcupacaoQuadraResponse;
import com.aracabeach.dto.ReservaFinanceiroResponse;
import com.aracabeach.repository.AulaRepository;
import com.aracabeach.repository.ComandaRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agrega numeros de todos os modulos (Reservas, Financeiro, Aulas, Loja)
 * para exibir uma visao geral do dia no Dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final QuadraRepository quadraRepository;
    private final ReservaRepository reservaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AulaRepository aulaRepository;
    private final ComandaRepository comandaRepository;
    private final PagamentoService pagamentoService;

    @Transactional(readOnly = true)
    public DashboardResponse gerar(LocalDate data) {
        LocalDateTime inicioDia = LocalDateTime.of(data, LocalTime.MIN);
        LocalDateTime fimDia = LocalDateTime.of(data, LocalTime.MAX);

        List<Reserva> reservasDoDia = reservaRepository.findByInicioBetween(inicioDia, fimDia);
        long reservasAtivas = reservasDoDia.stream().filter(r -> r.getStatus() != StatusReserva.CANCELADA).count();
        long reservasCanceladas = reservasDoDia.stream().filter(r -> r.getStatus() == StatusReserva.CANCELADA).count();

        BigDecimal faturamentoReservas = pagamentoRepository.findByCriadoEmBetween(inicioDia, fimDia).stream()
                .map(p -> p.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal faturamentoLoja = comandaRepository.findByFechadaTrueAndCriadoEmBetween(inicioDia, fimDia).stream()
                .flatMap(c -> c.getItens().stream())
                .map(ItemComanda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal faturamentoTotal = faturamentoReservas.add(faturamentoLoja);

        LocalDateTime agora = LocalDateTime.now();
        List<AulaResumoResponse> proximasAulas = aulaRepository.findByInicioBetween(inicioDia, fimDia).stream()
                .filter(a -> !data.isEqual(LocalDate.now()) || a.getInicio().isAfter(agora))
                .sorted((a, b) -> a.getInicio().compareTo(b.getInicio()))
                .limit(5)
                .map(this::paraResumoAula)
                .toList();

        List<ReservaFinanceiroResponse> reservasPendentes = pagamentoService.visaoFinanceiraDoDia(data).stream()
                .filter(r -> !"PAGO".equals(r.statusPagamento()) && r.statusReserva() != StatusReserva.CANCELADA)
                .toList();

        return new DashboardResponse(
                data,
                (int) quadraRepository.count(),
                (int) reservasAtivas,
                (int) reservasCanceladas,
                faturamentoReservas,
                faturamentoLoja,
                faturamentoTotal,
                proximasAulas,
                reservasPendentes
        );
    }

    /**
     * Faturamento (reservas + loja) dia a dia, dos ultimos N dias ate hoje.
     * Usado no grafico de tendencia do Dashboard.
     */
    @Transactional(readOnly = true)
    public List<EstatisticaDiaResponse> faturamentoUltimosDias(int dias) {
        LocalDate hoje = LocalDate.now();
        return java.util.stream.IntStream.rangeClosed(0, dias - 1)
                .mapToObj(i -> hoje.minusDays(dias - 1L - i))
                .map(this::estatisticaDoDia)
                .toList();
    }

    private EstatisticaDiaResponse estatisticaDoDia(LocalDate data) {
        LocalDateTime inicioDia = LocalDateTime.of(data, LocalTime.MIN);
        LocalDateTime fimDia = LocalDateTime.of(data, LocalTime.MAX);

        BigDecimal faturamentoReservas = pagamentoRepository.findByCriadoEmBetween(inicioDia, fimDia).stream()
                .map(p -> p.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal faturamentoLoja = comandaRepository.findByFechadaTrueAndCriadoEmBetween(inicioDia, fimDia).stream()
                .flatMap(c -> c.getItens().stream())
                .map(ItemComanda::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidadeReservas = (int) reservaRepository.findByInicioBetween(inicioDia, fimDia).stream()
                .filter(r -> r.getStatus() != StatusReserva.CANCELADA)
                .count();

        return new EstatisticaDiaResponse(data, faturamentoReservas, faturamentoLoja, faturamentoReservas.add(faturamentoLoja), quantidadeReservas);
    }

    /**
     * Quantidade de reservas (nao canceladas) por quadra, dentro de um periodo.
     * Usado no grafico de ocupacao por quadra do Dashboard.
     */
    @Transactional(readOnly = true)
    public List<OcupacaoQuadraResponse> ocupacaoPorQuadra(LocalDate inicio, LocalDate fim) {
        LocalDateTime inicioDateTime = LocalDateTime.of(inicio, LocalTime.MIN);
        LocalDateTime fimDateTime = LocalDateTime.of(fim, LocalTime.MAX);

        List<Reserva> reservas = reservaRepository.findByInicioBetween(inicioDateTime, fimDateTime).stream()
                .filter(r -> r.getStatus() != StatusReserva.CANCELADA)
                .toList();

        return reservas.stream()
                .collect(Collectors.groupingBy(r -> r.getQuadra().getNome(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new OcupacaoQuadraResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(OcupacaoQuadraResponse::quantidadeReservas).reversed())
                .toList();
    }

    private AulaResumoResponse paraResumoAula(Aula aula) {
        return new AulaResumoResponse(
                aula.getId(),
                aula.getProfessor().getNome(),
                aula.getQuadra().getNome(),
                aula.getInicio(),
                aula.getFim()
        );
    }
}
