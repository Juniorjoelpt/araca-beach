package com.aracabeach.service;

import com.aracabeach.domain.despesa.Despesa;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.produto.ItemComanda;
import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.quadra.StatusQuadra;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.CobrancaPendenteResponse;
import com.aracabeach.dto.GerencialResponse;
import com.aracabeach.dto.GerencialResponse.*;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ComandaRepository;
import com.aracabeach.repository.DespesaRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.QuadraRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Indicadores gerenciais (receita x despesa, ocupacao, cancelamentos, ranking, inadimplencia). */
@Service
@RequiredArgsConstructor
public class GerencialService {

    private static final int MAX_DIAS = 366;

    private final PagamentoRepository pagamentoRepository;
    private final ComandaRepository comandaRepository;
    private final DespesaRepository despesaRepository;
    private final ReservaRepository reservaRepository;
    private final QuadraRepository quadraRepository;
    private final ClienteRepository clienteRepository;
    private final PagamentoService pagamentoService;

    @Value("${araca-beach.portal.horario-abertura:6}")
    private int horarioAbertura;

    @Value("${araca-beach.portal.horario-fechamento:23}")
    private int horarioFechamento;

    @Transactional(readOnly = true)
    public GerencialResponse gerar(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        }
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        if (dias > MAX_DIAS) {
            throw new IllegalArgumentException("O período máximo é de " + MAX_DIAS + " dias.");
        }
        LocalDate fimAnt = inicio.minusDays(1);
        LocalDate inicioAnt = inicio.minusDays(dias);

        Dados atual = carregar(inicio, fim);
        Dados anterior = carregar(inicioAnt, fimAnt);

        Map<String, BigDecimal> porForma = atual.pagamentos.stream().collect(Collectors.groupingBy(
                p -> p.getFormaPagamento().name(),
                TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Pagamento::getValor, BigDecimal::add)));

        Map<String, BigDecimal> porCategoria = atual.despesas.stream().collect(Collectors.groupingBy(
                d -> d.getCategoria().name(),
                TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Despesa::getValor, BigDecimal::add)));

        return new GerencialResponse(inicio, fim, inicioAnt, fimAnt,
                indicadores(atual, inicio, fim), indicadores(anterior, inicioAnt, fimAnt),
                porForma, porCategoria,
                ocupacao(atual.reservasAtivas(), dias),
                mapaDeCalor(atual.reservasAtivas()),
                topClientes(atual.reservasAtivas()),
                inadimplencia(),
                receitaDiaria(atual, inicio, fim));
    }

    // ---------- carga ----------

    private record Dados(List<Pagamento> pagamentos, BigDecimal loja, List<Despesa> despesas,
                         Map<LocalDate, BigDecimal> lojaPorDia, List<Reserva> reservas) {
        List<Reserva> reservasAtivas() {
            return reservas.stream()
                    .filter(r -> r.getStatus() == StatusReserva.CONFIRMADA || r.getStatus() == StatusReserva.CONCLUIDA)
                    .toList();
        }
    }

    private Dados carregar(LocalDate inicio, LocalDate fim) {
        LocalDateTime di = LocalDateTime.of(inicio, LocalTime.MIN);
        LocalDateTime df = LocalDateTime.of(fim, LocalTime.MAX);

        List<Pagamento> pagamentos = pagamentoRepository.findByCriadoEmBetween(di, df).stream()
                .filter(p -> p.getStatus() != StatusPagamento.ESTORNADO)
                .toList();

        Map<LocalDate, BigDecimal> lojaPorDia = new HashMap<>();
        BigDecimal loja = BigDecimal.ZERO;
        for (var c : comandaRepository.findByFechadaTrueAndCriadoEmBetween(di, df)) {
            BigDecimal total = c.getItens().stream().map(ItemComanda::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            loja = loja.add(total);
            lojaPorDia.merge(c.getCriadoEm().toLocalDate(), total, BigDecimal::add);
        }

        List<Despesa> despesas = despesaRepository.findByPagaTrueAndDataPagamentoBetween(inicio, fim);
        List<Reserva> reservas = reservaRepository.findByInicioBetween(di, df);
        return new Dados(pagamentos, loja, despesas, lojaPorDia, reservas);
    }

    // ---------- calculos ----------

    private Indicadores indicadores(Dados d, LocalDate inicio, LocalDate fim) {
        BigDecimal caixa = soma(d.pagamentos.stream().map(Pagamento::getValor));
        BigDecimal receitaTotal = caixa.add(d.loja);
        BigDecimal despesas = soma(d.despesas.stream().map(Despesa::getValor));

        List<Reserva> ativas = d.reservasAtivas();
        int canceladas = (int) d.reservas.stream().filter(r -> r.getStatus() == StatusReserva.CANCELADA).count();
        int noShow = (int) d.reservas.stream().filter(r -> r.getStatus() == StatusReserva.NAO_COMPARECEU).count();
        int totalGeral = d.reservas.size();
        BigDecimal taxaCancel = totalGeral == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf((canceladas + noShow) * 100.0 / totalGeral).setScale(1, RoundingMode.HALF_UP);

        BigDecimal multas = soma(d.reservas.stream()
                .filter(r -> r.getStatus() == StatusReserva.CANCELADA || r.getStatus() == StatusReserva.NAO_COMPARECEU)
                .map(r -> r.getTaxaCancelamento() == null ? BigDecimal.ZERO : r.getTaxaCancelamento()));

        List<BigDecimal> comValor = ativas.stream().map(Reserva::getValorTotal)
                .filter(v -> v != null && v.signum() > 0).toList();
        BigDecimal ticket = comValor.isEmpty() ? BigDecimal.ZERO
                : soma(comValor.stream()).divide(BigDecimal.valueOf(comValor.size()), 2, RoundingMode.HALF_UP);

        long minutos = ativas.stream().mapToLong(r -> Duration.between(r.getInicio(), r.getFim()).toMinutes()).sum();
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);

        int novos = (int) clienteRepository.countByCriadoEmBetween(
                LocalDateTime.of(inicio, LocalTime.MIN), LocalDateTime.of(fim, LocalTime.MAX));

        return new Indicadores(caixa, d.loja, receitaTotal, despesas, receitaTotal.subtract(despesas),
                ativas.size(), canceladas, noShow, taxaCancel, multas, ticket, horas, novos);
    }

    private List<OcupacaoQuadraHoras> ocupacao(List<Reserva> ativas, long dias) {
        int horasDia = Math.max(0, horarioFechamento - horarioAbertura);
        BigDecimal disponiveis = BigDecimal.valueOf(horasDia * dias);

        Map<Long, Long> minutosPorQuadra = ativas.stream().collect(Collectors.groupingBy(
                r -> r.getQuadra().getId(),
                Collectors.summingLong(r -> Duration.between(r.getInicio(), r.getFim()).toMinutes())));

        List<OcupacaoQuadraHoras> lista = new ArrayList<>();
        for (Quadra q : quadraRepository.findAll()) {
            if (q.getStatus() != StatusQuadra.DISPONIVEL) continue;
            BigDecimal horas = BigDecimal.valueOf(minutosPorQuadra.getOrDefault(q.getId(), 0L))
                    .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
            BigDecimal pct = disponiveis.signum() == 0 ? BigDecimal.ZERO
                    : horas.multiply(BigDecimal.valueOf(100)).divide(disponiveis, 1, RoundingMode.HALF_UP);
            lista.add(new OcupacaoQuadraHoras(q.getNome(), horas, disponiveis, pct));
        }
        lista.sort(Comparator.comparing(OcupacaoQuadraHoras::ocupacaoPercentual).reversed());
        return lista;
    }

    private List<Heat> mapaDeCalor(List<Reserva> ativas) {
        Map<String, Integer> contagem = new TreeMap<>();
        for (Reserva r : ativas) {
            LocalDateTime t = r.getInicio().truncatedTo(ChronoUnit.HOURS);
            int guarda = 0;
            while (t.isBefore(r.getFim()) && guarda++ < 24) {
                String chave = t.getDayOfWeek().getValue() + "-" + t.getHour();
                contagem.merge(chave, 1, Integer::sum);
                t = t.plusHours(1);
            }
        }
        return contagem.entrySet().stream().map(e -> {
            String[] p = e.getKey().split("-");
            return new Heat(Integer.parseInt(p[0]), Integer.parseInt(p[1]), e.getValue());
        }).toList();
    }

    private List<TopCliente> topClientes(List<Reserva> ativas) {
        Map<Long, List<Reserva>> porCliente = ativas.stream()
                .filter(r -> r.getCliente() != null)
                .collect(Collectors.groupingBy(r -> r.getCliente().getId()));
        return porCliente.values().stream()
                .map(lista -> new TopCliente(
                        lista.get(0).getCliente().getId(),
                        lista.get(0).getCliente().getNome(),
                        lista.size(),
                        soma(lista.stream().map(r -> r.getValorTotal() == null ? BigDecimal.ZERO : r.getValorTotal()))))
                .sorted(Comparator.comparing(TopCliente::valor).thenComparing(TopCliente::reservas).reversed())
                .limit(5)
                .toList();
    }

    private Inadimplencia inadimplencia() {
        LocalDate hoje = LocalDate.now();
        BigDecimal atraso = BigDecimal.ZERO;
        BigDecimal aVencer = BigDecimal.ZERO;
        int qAtraso = 0;
        int qAVencer = 0;
        long maior = 0;
        for (CobrancaPendenteResponse c : pagamentoService.listarCobrancasPendentes()) {
            if (c.vencida()) {
                atraso = atraso.add(c.valor());
                qAtraso++;
                maior = Math.max(maior, ChronoUnit.DAYS.between(c.vencimento(), hoje));
            } else {
                aVencer = aVencer.add(c.valor());
                qAVencer++;
            }
        }
        return new Inadimplencia(atraso, qAtraso, aVencer, qAVencer, maior);
    }

    private List<ReceitaDia> receitaDiaria(Dados d, LocalDate inicio, LocalDate fim) {
        Map<LocalDate, BigDecimal> receita = new HashMap<>(d.lojaPorDia);
        d.pagamentos.forEach(p -> receita.merge(p.getCriadoEm().toLocalDate(), p.getValor(), BigDecimal::add));
        Map<LocalDate, BigDecimal> despesa = new HashMap<>();
        d.despesas.forEach(x -> despesa.merge(x.getDataPagamento(), x.getValor(), BigDecimal::add));

        List<ReceitaDia> lista = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            lista.add(new ReceitaDia(dia, receita.getOrDefault(dia, BigDecimal.ZERO),
                    despesa.getOrDefault(dia, BigDecimal.ZERO)));
        }
        return lista;
    }

    private static BigDecimal soma(java.util.stream.Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
