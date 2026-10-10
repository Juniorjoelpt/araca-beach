package com.aracabeach.service;

import com.aracabeach.domain.restaurante.*;
import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.repository.ComandaRestauranteRepository;
import com.aracabeach.repository.FichaTecnicaItemRepository;
import com.aracabeach.repository.InsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/** Relatorio gerencial do restaurante (comandas fechadas no periodo). */
@Service
@RequiredArgsConstructor
public class RestauranteRelatorioService {

    private static final int MAX_DIAS = 366;

    private final ComandaRestauranteRepository comandaRepository;
    private final FichaTecnicaItemRepository fichaRepository;
    private final InsumoRepository insumoRepository;

    @Transactional(readOnly = true)
    public RelatorioRestauranteResponse gerar(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        if (inicio.plusDays(MAX_DIAS).isBefore(fim)) throw new IllegalArgumentException("O período máximo é de " + MAX_DIAS + " dias.");

        List<ComandaRestaurante> comandas = comandaRepository.findByStatusAndFechadaEmBetween(
                StatusComandaRestaurante.FECHADA, LocalDateTime.of(inicio, LocalTime.MIN), LocalDateTime.of(fim, LocalTime.MAX));

        // Custo "de hoje" (ficha; sem ficha, insumo de mesmo nome) - so usado quando a venda e antiga e nao gravou o custo.
        Map<Long, BigDecimal> custoAtual = new HashMap<>();
        fichaRepository.findAll().stream()
                .collect(Collectors.groupingBy(f -> f.getItem().getId()))
                .forEach((id, ficha) -> custoAtual.put(id, CardapioService.custoDaFicha(ficha)));
        Map<String, BigDecimal> custoPorNome = new HashMap<>();
        insumoRepository.findAll().forEach(i -> custoPorNome.put(i.getNome().trim().toLowerCase(), i.custoDeUso()));
        Map<Long, String> semCusto = new LinkedHashMap<>();
        java.util.function.Function<ItemPedidoRestaurante, BigDecimal> custoDe = i -> {
            if (i.getCustoUnitario() != null) return i.getCustoUnitario();
            Long itemId = i.getItem().getId();
            BigDecimal c = custoAtual.get(itemId);
            if (c == null || c.signum() <= 0) c = custoPorNome.get(i.getNome().trim().toLowerCase());
            if (c == null || c.signum() <= 0) {
                semCusto.put(itemId, i.getNome());
                return BigDecimal.ZERO;
            }
            return c;
        };

        BigDecimal receitaItens = BigDecimal.ZERO, taxas = BigDecimal.ZERO, descontos = BigDecimal.ZERO, receitaTotal = BigDecimal.ZERO;
        BigDecimal custoTotal = BigDecimal.ZERO;
        Map<Long, long[]> qtdPorItem = new HashMap<>();
        Map<Long, BigDecimal> receitaPorItem = new HashMap<>();
        Map<Long, String> nomePorItem = new HashMap<>();
        Map<Long, BigDecimal> custoPorItem = new HashMap<>();
        long[] qtdHora = new long[24];
        BigDecimal[] recHora = new BigDecimal[24];
        long[] qtdDia = new long[8];
        BigDecimal[] recDia = new BigDecimal[8];
        Arrays.fill(recHora, BigDecimal.ZERO);
        Arrays.fill(recDia, BigDecimal.ZERO);

        int cortesias = 0;
        BigDecimal valorCortesias = BigDecimal.ZERO, custoCortesias = BigDecimal.ZERO;
        List<ComandaRestaurante> vendas = new ArrayList<>();
        for (ComandaRestaurante c : comandas) {
            if (c.isCortesia()) {
                // Cortesia nao e receita: so conta quantas foram, o valor de referencia e o custo dos itens.
                cortesias++;
                valorCortesias = valorCortesias.add(ComandaRestauranteService.subtotal(c));
                for (ItemPedidoRestaurante i : ComandaRestauranteService.itensAtivos(c)) {
                    custoCortesias = custoCortesias.add(custoDe.apply(i).multiply(BigDecimal.valueOf(i.getQuantidade())));
                }
            } else {
                vendas.add(c);
            }
        }
        comandas = vendas;

        for (ComandaRestaurante c : comandas) {
            BigDecimal sub = ComandaRestauranteService.subtotal(c);
            receitaItens = receitaItens.add(sub);
            taxas = taxas.add(ComandaRestauranteService.taxa(c, sub));
            descontos = descontos.add(c.getDescontoValor());
            receitaTotal = receitaTotal.add(ComandaRestauranteService.total(c));

            for (PedidoRestaurante p : c.getPedidos()) {
                for (ItemPedidoRestaurante i : p.getItens()) {
                    if (i.isCancelado()) continue;
                    Long itemId = i.getItem().getId();
                    nomePorItem.put(itemId, i.getNome());
                    qtdPorItem.computeIfAbsent(itemId, k -> new long[1])[0] += i.getQuantidade();
                    receitaPorItem.merge(itemId, i.getSubtotal(), BigDecimal::add);
                    BigDecimal custoLinha = custoDe.apply(i).multiply(BigDecimal.valueOf(i.getQuantidade()));
                    custoTotal = custoTotal.add(custoLinha);
                    custoPorItem.merge(itemId, custoLinha, BigDecimal::add);
                    int h = p.getCriadoEm().getHour();
                    int d = p.getCriadoEm().getDayOfWeek().getValue();
                    qtdHora[h] += i.getQuantidade();
                    recHora[h] = recHora[h].add(i.getSubtotal());
                    qtdDia[d] += i.getQuantidade();
                    recDia[d] = recDia[d].add(i.getSubtotal());
                }
            }
        }

        BigDecimal lucro = receitaItens.subtract(descontos).subtract(custoTotal);

        List<ItemVendidoResponse> maisVendidos = qtdPorItem.entrySet().stream()
                .map(e -> {
                    BigDecimal receita = receitaPorItem.get(e.getKey());
                    BigDecimal custo = custoPorItem.getOrDefault(e.getKey(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                    return new ItemVendidoResponse(e.getKey(), nomePorItem.get(e.getKey()), e.getValue()[0], receita,
                            custo, receita.subtract(custo));
                })
                .sorted(Comparator.comparingLong(ItemVendidoResponse::quantidade).reversed())
                .limit(15)
                .toList();

        List<FaixaResponse> porHora = new ArrayList<>();
        for (int h = 0; h < 24; h++) if (qtdHora[h] > 0) porHora.add(new FaixaResponse(h, qtdHora[h], recHora[h]));
        List<FaixaResponse> porDia = new ArrayList<>();
        for (int d = 1; d <= 7; d++) if (qtdDia[d] > 0) porDia.add(new FaixaResponse(d, qtdDia[d], recDia[d]));

        BigDecimal ticket = comandas.isEmpty() ? BigDecimal.ZERO
                : receitaTotal.divide(BigDecimal.valueOf(comandas.size()), 2, RoundingMode.HALF_UP);

        List<String> abaixo = insumoRepository.findAllByOrderByNomeAsc().stream()
                .filter(i -> i.isAtivo() && i.getEstoqueMinimo().signum() > 0 && i.getEstoqueAtual().compareTo(i.getEstoqueMinimo()) <= 0)
                .map(Insumo::getNome).toList();

        return new RelatorioRestauranteResponse(inicio, fim, comandas.size(), receitaItens, taxas, descontos, receitaTotal,
                ticket, custoTotal.setScale(2, RoundingMode.HALF_UP), lucro.setScale(2, RoundingMode.HALF_UP),
                semCusto.size(), maisVendidos, porHora, porDia, abaixo,
                cortesias, valorCortesias, custoCortesias.setScale(2, RoundingMode.HALF_UP),
                new ArrayList<>(semCusto.values()), lucro.subtract(custoCortesias).setScale(2, RoundingMode.HALF_UP));
    }
}
