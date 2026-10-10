package com.aracabeach.service;

import com.aracabeach.domain.restaurante.*;
import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.FichaTecnicaItemRepository;
import com.aracabeach.repository.InsumoRepository;
import com.aracabeach.repository.MovimentoInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Estoque de insumos do restaurante: cadastro, entradas/perdas/ajustes e baixa automatica pela ficha tecnica. */
@Service
@RequiredArgsConstructor
public class InsumoService {

    private final InsumoRepository insumoRepository;
    private final MovimentoInsumoRepository movimentoRepository;
    private final FichaTecnicaItemRepository fichaRepository;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<InsumoResponse> listar() {
        return insumoRepository.findAllByOrderByNomeAsc().stream().map(this::paraResponse).toList();
    }

    @Transactional
    public InsumoResponse criar(InsumoRequest r) {
        Insumo i = Insumo.builder()
                .nome(r.nome().trim())
                .unidade(r.unidade())
                .estoqueMinimo(valor(r.estoqueMinimo()))
                .custoUnitario(valor(r.custoUnitario()))
                .unidadesPorEmbalagem(r.unidadesPorEmbalagem())
                .ativo(r.ativo() == null || r.ativo())
                .build();
        return paraResponse(insumoRepository.save(i));
    }

    @Transactional
    public InsumoResponse atualizar(Long id, InsumoRequest r) {
        Insumo i = buscar(id);
        if (i.getUnidade() != r.unidade() && fichaRepository.existsByInsumoId(id)) {
            throw new IllegalArgumentException("Não é possível trocar a unidade de um insumo usado em ficha técnica.");
        }
        i.setNome(r.nome().trim());
        i.setUnidade(r.unidade());
        i.setEstoqueMinimo(valor(r.estoqueMinimo()));
        i.setCustoUnitario(valor(r.custoUnitario()));
        i.setUnidadesPorEmbalagem(r.unidadesPorEmbalagem());
        if (r.ativo() != null) i.setAtivo(r.ativo());
        return paraResponse(insumoRepository.save(i));
    }

    @Transactional(readOnly = true)
    public List<MovimentoInsumoResponse> movimentos(Long insumoId) {
        return movimentoRepository.findTop50ByInsumoIdOrderByCriadoEmDesc(insumoId).stream()
                .map(m -> new MovimentoInsumoResponse(m.getId(), m.getTipo(), m.getQuantidade(), m.getObservacao(),
                        m.getComandaId(), m.getCriadoEm()))
                .toList();
    }

    /** ENTRADA soma; PERDA subtrai; AJUSTE define o estoque para o valor informado (contagem). */
    @Transactional
    public InsumoResponse movimentar(Long id, MovimentoInsumoRequest r) {
        Insumo i = buscar(id);
        BigDecimal qtd = r.quantidade().setScale(3, RoundingMode.HALF_UP);
        BigDecimal delta;
        switch (r.tipo()) {
            case ENTRADA -> {
                if (qtd.signum() <= 0) throw new IllegalArgumentException("Informe uma quantidade maior que zero.");
                delta = qtd;
                if (r.custoUnitario() != null && r.custoUnitario().signum() > 0) {
                    i.setCustoUnitario(r.custoUnitario()); // ultimo custo de compra
                }
            }
            case PERDA -> {
                if (qtd.signum() <= 0) throw new IllegalArgumentException("Informe uma quantidade maior que zero.");
                delta = qtd.negate();
            }
            case AJUSTE -> delta = qtd.subtract(i.getEstoqueAtual());
            default -> throw new IllegalArgumentException("Tipo de movimento não permitido manualmente.");
        }
        i.setEstoqueAtual(i.getEstoqueAtual().add(delta));
        insumoRepository.save(i);
        movimentoRepository.save(MovimentoInsumo.builder()
                .insumo(i).tipo(r.tipo()).quantidade(delta).observacao(r.observacao()).build());
        auditoria.detalhe(r.tipo() + " de insumo '" + i.getNome() + "': " + delta.stripTrailingZeros().toPlainString()
                + " " + i.getUnidade() + (r.observacao() != null && !r.observacao().isBlank() ? " - " + r.observacao() : ""));
        return paraResponse(i);
    }

    /**
     * Custo de producao de 1 unidade do item no momento da venda: ficha tecnica; sem ficha, o insumo de mesmo
     * nome (bebida revendida). Vazio quando nao ha como saber (custo zero ou inexistente).
     */
    @Transactional(readOnly = true)
    public java.util.Optional<BigDecimal> custoUnitarioDoItem(ItemCardapio item) {
        List<FichaTecnicaItem> ficha = fichaRepository.findByItemId(item.getId());
        BigDecimal custo = CardapioService.custoDaFicha(ficha);
        if (ficha.isEmpty() && item.getNome() != null) {
            custo = insumoRepository.findFirstByNomeIgnoreCase(item.getNome().trim())
                    .map(Insumo::custoDeUso).orElse(BigDecimal.ZERO);
        }
        return custo.signum() > 0 ? java.util.Optional.of(custo) : java.util.Optional.empty();
    }

    /** Baixa de estoque pela ficha tecnica de um item vendido. Itens sem ficha nao movimentam estoque. */
    @Transactional
    public void consumir(ItemCardapio item, int quantidade, Long comandaId) {
        aplicar(item, quantidade, comandaId, true);
    }

    /** Devolve ao estoque o que foi baixado (item cancelado). */
    @Transactional
    public void estornar(ItemCardapio item, int quantidade, Long comandaId) {
        aplicar(item, quantidade, comandaId, false);
    }

    private void aplicar(ItemCardapio item, int quantidade, Long comandaId, boolean consumo) {
        List<FichaTecnicaItem> ficha = fichaRepository.findByItemId(item.getId());
        List<Object[]> linhas = new java.util.ArrayList<>(); // {insumo, quantidade de uso por unidade vendida}
        for (FichaTecnicaItem l : ficha) linhas.add(new Object[]{l.getInsumo(), l.getQuantidade()});
        if (linhas.isEmpty() && item.getNome() != null) {
            // Sem ficha tecnica: se existir insumo com o mesmo nome (ex.: bebida revendida), baixa 1 por unidade vendida.
            insumoRepository.findFirstByNomeIgnoreCase(item.getNome().trim())
                    .ifPresent(i -> linhas.add(new Object[]{i, BigDecimal.ONE}));
        }
        for (Object[] linha : linhas) {
            Insumo insumo = (Insumo) linha[0];
            // quantidade de uso -> unidade de estoque (ex.: 3 latas = 0,125 de um fardo de 24)
            BigDecimal qtd = ((BigDecimal) linha[1]).multiply(BigDecimal.valueOf(quantidade))
                    .divide(insumo.fatorUso(), 6, RoundingMode.HALF_UP);
            if (qtd.signum() == 0) continue;
            BigDecimal delta = consumo ? qtd.negate() : qtd;
            insumo.setEstoqueAtual(insumo.getEstoqueAtual().add(delta));
            insumoRepository.save(insumo);
            movimentoRepository.save(MovimentoInsumo.builder()
                    .insumo(insumo)
                    .tipo(consumo ? TipoMovimentoInsumo.CONSUMO : TipoMovimentoInsumo.ESTORNO_CONSUMO)
                    .quantidade(delta)
                    .observacao(quantidade + "x " + item.getNome())
                    .comandaId(comandaId)
                    .build());
        }
    }

    private Insumo buscar(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Insumo não encontrado: " + id));
    }

    private static BigDecimal valor(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private InsumoResponse paraResponse(Insumo i) {
        boolean abaixo = i.getEstoqueMinimo().signum() > 0 && i.getEstoqueAtual().compareTo(i.getEstoqueMinimo()) <= 0;
        return new InsumoResponse(i.getId(), i.getNome(), i.getUnidade(), i.getEstoqueAtual(), i.getEstoqueMinimo(),
                i.getCustoUnitario(), i.isAtivo(), abaixo, i.getUnidadesPorEmbalagem());
    }
}
