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

        Map<Long, BigDecimal> custoUnitario = new HashMap<>();
        fichaRepository.findAll().stream()
                .collect(Collectors.groupingBy(f -> f.getItem().getId()))
                .forEach((id, ficha) -> custoUnitario.put(id, CardapioService.custoDaFicha(ficha)));

        BigDecimal receitaItens = BigDecimal.ZERO, taxas = BigDecimal.ZERO, descontos = BigDecimal.ZERO, receitaTotal = BigDecimal.ZERO;
        BigDecimal custoTotal = BigDecimal.ZERO;
        Map<Long, long[]> qtdPorItem = new HashMap<>();
        Map<Long, BigDecimal> receitaPorItem = new HashMap<>();
        Map<Long, String> nomePorItem = new HashMap<>();
        long[] qtdHora = new long[24];
        BigDecimal[] recHora = new BigDecimal[24];
        long[] qtdDia = new long[8];
        BigDecimal[] recDia = new BigDecimal[8];
        Arrays.fill(recHora, BigDecimal.ZERO);
        Arrays.fill(recDia, BigDecimal.ZERO);

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
                    custoTotal = custoTotal.add(custoUnitario.getOrDefault(itemId, BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(i.getQuantidade())));
                    int h = p.getCriadoEm().getHour();
                    int d = p.getCriadoEm().getDayOfWeek().getValue();
                    qtdHora[h] += i.getQuantidade();
                    recHora[h] = recHora[h].add(i.getSubtotal());
                    qtdDia[d] += i.getQuantidade();
                    recDia[d] = recDia[d].add(i.getSubtotal());
                }
            }
        }

        int semFicha = (int) qtdPorItem.keySet().stream().filter(id -> !custoUnitario.containsKey(id)).count();

        List<ItemVendidoResponse> maisVendidos = qtdPorItem.entrySet().stream()
                .map(e -> {
                    BigDecimal receita = receitaPorItem.get(e.getKey());
                    BigDecimal custo = custoUnitario.getOrDefault(e.getKey(), BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(e.getValue()[0])).setScale(2, RoundingMode.HALF_UP);
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
                ticket, custoTotal.setScale(2, RoundingMode.HALF_UP), receitaItens.subtract(custoTotal).setScale(2, RoundingMode.HALF_UP),
                semFicha, maisVendidos, porHora, porDia, abaixo);
    }
}
