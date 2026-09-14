package com.aracabeach.service;

import com.aracabeach.domain.estoque.MovimentacaoEstoque;
import com.aracabeach.domain.estoque.TipoMovimentacao;
import com.aracabeach.domain.produto.Produto;
import com.aracabeach.dto.AjusteEstoqueRequest;
import com.aracabeach.dto.MovimentacaoEstoqueRequest;
import com.aracabeach.dto.MovimentacaoEstoqueResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.MovimentacaoEstoqueRepository;
import com.aracabeach.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Unico lugar do sistema que deve alterar o campo "estoque" de um Produto -
 * qualquer entrada, saida, ajuste ou venda passa por aqui, garantindo que
 * toda mudanca fique registrada em MovimentacaoEstoque (auditoria completa
 * de "quem mudou o que e por que").
 */
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    @Transactional
    public MovimentacaoEstoqueResponse registrarEntrada(MovimentacaoEstoqueRequest request) {
        Produto produto = buscarProduto(request.produtoId());
        produto.setEstoque(produto.getEstoque() + request.quantidade());
        produtoRepository.save(produto);

        MovimentacaoEstoque movimentacao = salvarMovimentacao(produto, TipoMovimentacao.ENTRADA, request.quantidade(), request.motivo());
        return paraResponse(movimentacao);
    }

    @Transactional
    public MovimentacaoEstoqueResponse registrarSaida(MovimentacaoEstoqueRequest request) {
        Produto produto = buscarProduto(request.produtoId());
        if (produto.getEstoque() < request.quantidade()) {
            throw new IllegalArgumentException("Estoque insuficiente: há apenas " + produto.getEstoque() + " unidade(s) em estoque.");
        }
        produto.setEstoque(produto.getEstoque() - request.quantidade());
        produtoRepository.save(produto);

        MovimentacaoEstoque movimentacao = salvarMovimentacao(produto, TipoMovimentacao.SAIDA, request.quantidade(), request.motivo());
        return paraResponse(movimentacao);
    }

    @Transactional
    public MovimentacaoEstoqueResponse registrarAjuste(AjusteEstoqueRequest request) {
        Produto produto = buscarProduto(request.produtoId());
        int diferenca = request.novoEstoque() - produto.getEstoque();
        produto.setEstoque(request.novoEstoque());
        produtoRepository.save(produto);

        String motivoCompleto = (request.motivo() != null && !request.motivo().isBlank() ? request.motivo() + " — " : "")
                + "ajuste de " + produto.getEstoque() + " (diferença: " + (diferenca >= 0 ? "+" : "") + diferenca + ")";
        MovimentacaoEstoque movimentacao = salvarMovimentacao(produto, TipoMovimentacao.AJUSTE, Math.abs(diferenca), motivoCompleto);
        return paraResponse(movimentacao);
    }

    /**
     * Chamado pelo ComandaService quando um item e adicionado a uma comanda -
     * debita o estoque automaticamente e registra a movimentacao do tipo
     * VENDA, fechando a lacuna que existia (o estoque so mudava se alguem
     * editasse manualmente o produto).
     */
    @Transactional
    public void baixarPorVenda(Produto produto, int quantidade, Long comandaId) {
        if (produto.getEstoque() < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente para \"" + produto.getNome() + "\": há apenas " + produto.getEstoque() + " unidade(s).");
        }
        produto.setEstoque(produto.getEstoque() - quantidade);
        produtoRepository.save(produto);
        salvarMovimentacao(produto, TipoMovimentacao.VENDA, quantidade, "Venda — comanda #" + comandaId);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponse> listarMovimentacoes(Long produtoId, LocalDate inicio, LocalDate fim) {
        LocalDateTime inicioDateTime = LocalDateTime.of(inicio, LocalTime.MIN);
        LocalDateTime fimDateTime = LocalDateTime.of(fim, LocalTime.MAX);

        List<MovimentacaoEstoque> movimentacoes = produtoId != null
                ? movimentacaoRepository.findByProdutoIdAndCriadoEmBetweenOrderByCriadoEmDesc(produtoId, inicioDateTime, fimDateTime)
                : movimentacaoRepository.findByCriadoEmBetweenOrderByCriadoEmDesc(inicioDateTime, fimDateTime);

        return movimentacoes.stream().map(this::paraResponse).toList();
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    private MovimentacaoEstoque salvarMovimentacao(Produto produto, TipoMovimentacao tipo, int quantidade, String motivo) {
        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto)
                .tipo(tipo)
                .quantidade(quantidade)
                .motivo(motivo)
                .build();
        return movimentacaoRepository.save(movimentacao);
    }

    private MovimentacaoEstoqueResponse paraResponse(MovimentacaoEstoque m) {
        return new MovimentacaoEstoqueResponse(m.getId(), m.getProduto().getNome(), m.getTipo().name(), m.getQuantidade(), m.getMotivo(), m.getCriadoEm());
    }
}
