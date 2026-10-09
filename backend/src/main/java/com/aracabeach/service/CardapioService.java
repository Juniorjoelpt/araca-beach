package com.aracabeach.service;

import com.aracabeach.domain.restaurante.*;
import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/** Cardapio do restaurante (categorias e itens), pausa de itens e ficha tecnica com custo/margem. */
@Service
@RequiredArgsConstructor
public class CardapioService {

    private final CategoriaCardapioRepository categoriaRepository;
    private final ItemCardapioRepository itemRepository;
    private final InsumoRepository insumoRepository;
    private final FichaTecnicaItemRepository fichaRepository;
    private final AuditoriaService auditoria;

    // ---------- leitura para o caixa ----------

    /** Somente categorias e itens ativos, na ordem do cardapio. Itens pausados aparecem marcados. */
    @Transactional(readOnly = true)
    public List<CardapioCategoriaResponse> cardapioAtivo() {
        Map<Long, List<ItemCardapio>> porCategoria = itemRepository.findAll().stream()
                .filter(ItemCardapio::isAtivo)
                .collect(Collectors.groupingBy(i -> i.getCategoria().getId()));
        return categoriaRepository.findAllByOrderByOrdemAscNomeAsc().stream()
                .filter(CategoriaCardapio::isAtiva)
                .map(c -> new CardapioCategoriaResponse(c.getId(), c.getNome(), c.getOrdem(),
                        porCategoria.getOrDefault(c.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(ItemCardapio::getOrdem).thenComparing(ItemCardapio::getNome))
                                .map(i -> new CardapioItemResponse(i.getId(), i.getNome(), i.getDescricao(), i.getPorcao(),
                                        i.getPreco(), i.getPraca(), i.isPausado(), i.getTempoPreparoMin(),
                                        i.getCodigoBarras()))
                                .toList()))
                .filter(c -> !c.itens().isEmpty())
                .toList();
    }

    // ---------- categorias ----------

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAllByOrderByOrdemAscNomeAsc().stream().map(this::paraResponse).toList();
    }

    @Transactional
    public CategoriaResponse criarCategoria(CategoriaRequest r) {
        CategoriaCardapio c = CategoriaCardapio.builder()
                .nome(r.nome().trim())
                .ordem(r.ordem() == null ? 0 : r.ordem())
                .ativa(r.ativa() == null || r.ativa())
                .build();
        return paraResponse(categoriaRepository.save(c));
    }

    @Transactional
    public CategoriaResponse atualizarCategoria(Long id, CategoriaRequest r) {
        CategoriaCardapio c = buscarCategoria(id);
        c.setNome(r.nome().trim());
        if (r.ordem() != null) c.setOrdem(r.ordem());
        if (r.ativa() != null) c.setAtiva(r.ativa());
        return paraResponse(categoriaRepository.save(c));
    }

    // ---------- itens ----------

    @Transactional(readOnly = true)
    public List<ItemGestaoResponse> listarItens() {
        Map<Long, List<FichaTecnicaItem>> fichas = fichaRepository.findAll().stream()
                .collect(Collectors.groupingBy(f -> f.getItem().getId()));
        return itemRepository.findAll().stream()
                .sorted(Comparator.comparingInt((ItemCardapio i) -> i.getCategoria().getOrdem())
                        .thenComparingInt(ItemCardapio::getOrdem).thenComparing(ItemCardapio::getNome))
                .map(i -> paraGestao(i, fichas.getOrDefault(i.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ItemGestaoResponse criarItem(ItemCardapioRequest r) {
        ItemCardapio i = ItemCardapio.builder().build();
        aplicar(i, r);
        return paraGestao(itemRepository.save(i), List.of());
    }

    @Transactional
    public ItemGestaoResponse atualizarItem(Long id, ItemCardapioRequest r) {
        ItemCardapio i = buscarItem(id);
        BigDecimal precoAntes = i.getPreco();
        aplicar(i, r);
        itemRepository.save(i);
        if (precoAntes.compareTo(i.getPreco()) != 0) {
            auditoria.detalhe("Preço de '" + i.getNome() + "' alterado de R$ " + precoAntes + " para R$ " + i.getPreco());
        }
        return paraGestao(i, fichaRepository.findByItemId(id));
    }

    @Transactional
    public ItemGestaoResponse pausar(Long id, boolean pausado) {
        ItemCardapio i = buscarItem(id);
        i.setPausado(pausado);
        itemRepository.save(i);
        auditoria.detalhe("'" + i.getNome() + "' " + (pausado ? "pausado (acabou)" : "voltou ao cardápio"));
        return paraGestao(i, fichaRepository.findByItemId(id));
    }

    /** Substitui a ficha tecnica do item (lista vazia remove a ficha). */
    @Transactional
    public ItemGestaoResponse definirFicha(Long id, List<FichaLinhaRequest> linhas) {
        ItemCardapio item = buscarItem(id);
        Set<Long> vistos = new HashSet<>();
        List<FichaTecnicaItem> novas = new ArrayList<>();
        for (FichaLinhaRequest l : linhas) {
            if (!vistos.add(l.insumoId())) {
                throw new IllegalArgumentException("O mesmo insumo foi informado duas vezes na ficha.");
            }
            Insumo insumo = insumoRepository.findById(l.insumoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Insumo não encontrado: " + l.insumoId()));
            novas.add(FichaTecnicaItem.builder().item(item).insumo(insumo).quantidade(l.quantidade()).build());
        }
        fichaRepository.deleteAll(fichaRepository.findByItemId(id));
        fichaRepository.flush();
        fichaRepository.saveAll(novas);
        return paraGestao(item, novas);
    }

    // ---------- custo ----------

    /** Custo de producao de 1 unidade do item, a partir da ficha tecnica (zero se nao ha ficha). */
    public static BigDecimal custoDaFicha(List<FichaTecnicaItem> ficha) {
        return ficha.stream()
                .map(f -> f.getQuantidade().multiply(f.getInsumo().getCustoUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- internos ----------

    private void aplicar(ItemCardapio i, ItemCardapioRequest r) {
        String codigo = normalizarCodigo(r.codigoBarras());
        if (codigo != null) {
            itemRepository.findByCodigoBarras(codigo)
                    .filter(outro -> !Objects.equals(outro.getId(), i.getId()))
                    .ifPresent(outro -> {
                        throw new IllegalArgumentException("O código de barras " + codigo + " já está cadastrado no item '" + outro.getNome() + "'.");
                    });
        }
        i.setCodigoBarras(codigo);
        i.setCategoria(buscarCategoria(r.categoriaId()));
        i.setNome(r.nome().trim());
        i.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        i.setPorcao(r.porcao() == null || r.porcao().isBlank() ? null : r.porcao().trim());
        i.setPreco(r.preco());
        if (r.praca() != null) i.setPraca(r.praca());
        if (r.ativo() != null) i.setAtivo(r.ativo());
        if (r.pausado() != null) i.setPausado(r.pausado());
        i.setTempoPreparoMin(r.tempoPreparoMin());
        if (r.ordem() != null) i.setOrdem(r.ordem());
    }

    /** Remove espacos e hifens; vazio vira null (varios itens podem ficar sem codigo). */
    private static String normalizarCodigo(String codigo) {
        if (codigo == null) return null;
        String limpo = codigo.replaceAll("[\\s-]", "");
        return limpo.isEmpty() ? null : limpo;
    }

    private CategoriaCardapio buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }

    private ItemCardapio buscarItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do cardápio não encontrado: " + id));
    }

    private CategoriaResponse paraResponse(CategoriaCardapio c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getOrdem(), c.isAtiva());
    }

    private ItemGestaoResponse paraGestao(ItemCardapio i, List<FichaTecnicaItem> ficha) {
        BigDecimal custo = custoDaFicha(ficha);
        BigDecimal margem = i.getPreco().subtract(custo);
        BigDecimal margemPct = i.getPreco().signum() == 0 ? BigDecimal.ZERO
                : margem.multiply(BigDecimal.valueOf(100)).divide(i.getPreco(), 1, RoundingMode.HALF_UP);
        List<FichaLinhaResponse> linhas = ficha.stream().map(f -> new FichaLinhaResponse(
                f.getInsumo().getId(), f.getInsumo().getNome(), f.getInsumo().getUnidade(), f.getQuantidade(),
                f.getQuantidade().multiply(f.getInsumo().getCustoUnitario()).setScale(2, RoundingMode.HALF_UP))).toList();
        return new ItemGestaoResponse(i.getId(), i.getCategoria().getId(), i.getCategoria().getNome(), i.getNome(),
                i.getDescricao(), i.getPorcao(), i.getPreco(), i.getPraca(), i.isAtivo(), i.isPausado(),
                i.getTempoPreparoMin(), i.getOrdem(), custo, margem, margemPct, !ficha.isEmpty(), linhas, i.getCodigoBarras());
    }
}
