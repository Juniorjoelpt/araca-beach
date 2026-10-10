package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.produto.Comanda;
import com.aracabeach.domain.produto.ItemComanda;
import com.aracabeach.domain.produto.Produto;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.dto.ComandaRequest;
import com.aracabeach.dto.ItemComandaRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.config.OperadorAtual;
import com.aracabeach.domain.financeiro.FormaPagamento;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.repository.ComandaRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.ProdutoRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComandaService {

    private final ComandaRepository comandaRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public Comanda abrir(ComandaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + request.clienteId()));

        Reserva reserva = null;
        if (request.reservaId() != null) {
            reserva = reservaRepository.findById(request.reservaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada: " + request.reservaId()));
        }

        Comanda comanda = Comanda.builder()
                .cliente(cliente)
                .reserva(reserva)
                .fechada(false)
                .build();

        return comandaRepository.save(comanda);
    }

    @Transactional
    public Comanda adicionarItem(Long comandaId, ItemComandaRequest request) {
        Comanda comanda = buscarPorId(comandaId);
        if (comanda.isFechada()) {
            throw new IllegalStateException("Nao e possivel adicionar itens a uma comanda ja fechada.");
        }

        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto nao encontrado: " + request.produtoId()));

        ItemComanda item = ItemComanda.builder()
                .comanda(comanda)
                .produto(produto)
                .quantidade(request.quantidade())
                .precoUnitario(produto.getPreco())
                .custoUnitario(produto.getCusto())
                .build();

        // Debita o estoque e registra a movimentacao automaticamente - antes
        // o campo "estoque" so mudava se alguem editasse o produto na mao.
        estoqueService.baixarPorVenda(produto, request.quantidade(), comandaId);

        comanda.getItens().add(item);
        return comandaRepository.save(comanda);
    }

    @Transactional
    public Comanda fechar(Long comandaId, FormaPagamento forma) {
        Comanda comanda = buscarPorId(comandaId);
        if (comanda.isFechada()) {
            throw new IllegalArgumentException("Esta comanda ja foi fechada.");
        }
        BigDecimal total = comanda.getTotal();
        if (total.signum() > 0 && forma == null) {
            throw new IllegalArgumentException("Informe a forma de pagamento para fechar a comanda.");
        }
        String operador = OperadorAtual.login();
        comanda.setFechada(true);
        comanda.setFechadaEm(LocalDateTime.now());
        comanda.setOperador(operador);
        comanda.setFormaPagamento(total.signum() > 0 ? forma : null);
        // A venda da loja entra no caixa como os demais recebimentos.
        if (total.signum() > 0) {
            pagamentoRepository.save(Pagamento.builder()
                    .reserva(null)
                    .valor(total)
                    .formaPagamento(forma)
                    .status(StatusPagamento.PAGO)
                    .ehSinal(false)
                    .origem("LOJA")
                    .descricao("Loja - Comanda #" + comandaId + " - " + comanda.getCliente().getNome())
                    .operador(operador)
                    .build());
        }
        return comandaRepository.save(comanda);
    }

    @Transactional(readOnly = true)
    public Comanda buscarPorId(Long id) {
        return comandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda nao encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<Comanda> listarAbertas() {
        return comandaRepository.findByFechadaFalse();
    }
}
