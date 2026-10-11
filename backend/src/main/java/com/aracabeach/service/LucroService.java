package com.aracabeach.service;

import com.aracabeach.domain.produto.Comanda;
import com.aracabeach.domain.produto.ItemComanda;
import com.aracabeach.dto.LucroResponse;
import com.aracabeach.dto.RestauranteDtos.RelatorioRestauranteResponse;
import com.aracabeach.repository.ComandaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.TreeSet;

/** Lucro das vendas do restaurante e da loja. */
@Service
@RequiredArgsConstructor
public class LucroService {

    private final RestauranteRelatorioService restauranteRelatorio;
    private final ComandaRepository lojaRepository;
    private final com.aracabeach.repository.DayUseVendaRepository dayUseRepository;

    @Transactional(readOnly = true)
    public LucroResponse gerar(LocalDate inicio, LocalDate fim) {
        RelatorioRestauranteResponse r = restauranteRelatorio.gerar(inicio, fim);
        LucroResponse.Restaurante rest = new LucroResponse.Restaurante(r.comandas(), r.receitaItens(), r.descontos(),
                r.custoTotal(), r.margemTotal(), r.cortesias(), r.custoCortesias(), r.lucroAposCortesias(), r.itensSemCusto());

        List<Comanda> vendas = lojaRepository.findByFechadaTrueAndFechadaEmBetween(
                LocalDateTime.of(inicio, LocalTime.MIN), LocalDateTime.of(fim, LocalTime.MAX));
        BigDecimal receita = BigDecimal.ZERO, custo = BigDecimal.ZERO;
        TreeSet<String> semCusto = new TreeSet<>();
        for (Comanda c : vendas) {
            receita = receita.add(c.getTotal());
            for (ItemComanda i : c.getItens()) {
                if (i.getProduto().isEhAluguel()) continue; // aluguel nao tem custo de mercadoria
                BigDecimal unit = i.getCustoUnitario() != null ? i.getCustoUnitario() : i.getProduto().getCusto();
                if (unit == null || unit.signum() <= 0) {
                    semCusto.add(i.getProduto().getNome());
                    continue;
                }
                custo = custo.add(unit.multiply(BigDecimal.valueOf(i.getQuantidade())));
            }
        }
        receita = receita.setScale(2, RoundingMode.HALF_UP);
        custo = custo.setScale(2, RoundingMode.HALF_UP);
        LucroResponse.Loja loja = new LucroResponse.Loja(vendas.size(), receita, custo, receita.subtract(custo), List.copyOf(semCusto));

        // Day Use: pelo dia do pagamento (mesmo criterio do caixa). Custo so quando informado no ingresso.
        List<com.aracabeach.domain.dayuse.DayUseVenda> dayUseVendas = dayUseRepository.findByStatusAndPagoEmBetween(
                com.aracabeach.domain.dayuse.StatusDayUse.ATIVA, LocalDateTime.of(inicio, LocalTime.MIN), LocalDateTime.of(fim, LocalTime.MAX));
        BigDecimal duReceita = BigDecimal.ZERO, duCusto = BigDecimal.ZERO;
        int duPessoas = 0;
        for (com.aracabeach.domain.dayuse.DayUseVenda v : dayUseVendas) {
            duReceita = duReceita.add(v.getValorTotal());
            duPessoas += v.getPessoas();
            for (com.aracabeach.domain.dayuse.DayUseItem i : v.getItens()) {
                if (i.getCustoUnitario() != null) duCusto = duCusto.add(i.getCustoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())));
            }
        }
        duReceita = duReceita.setScale(2, RoundingMode.HALF_UP);
        duCusto = duCusto.setScale(2, RoundingMode.HALF_UP);
        LucroResponse.DayUse dayUse = new LucroResponse.DayUse(dayUseVendas.size(), duPessoas, duReceita, duCusto, duReceita.subtract(duCusto));

        BigDecimal receitaTotal = rest.receita().subtract(rest.descontos()).add(receita).add(duReceita);
        BigDecimal custoTotal = rest.custo().add(rest.perdaCortesias()).add(custo).add(duCusto);
        return new LucroResponse(inicio, fim, rest, loja, dayUse, receitaTotal, custoTotal,
                rest.lucro().add(loja.lucro()).add(dayUse.lucro()));
    }
}
