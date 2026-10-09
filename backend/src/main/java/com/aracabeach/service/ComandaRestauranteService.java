package com.aracabeach.service;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.restaurante.*;
import com.aracabeach.dto.RestauranteDtos.*;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.ComandaRestauranteRepository;
import com.aracabeach.repository.ItemCardapioRepository;
import com.aracabeach.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comandas do restaurante: abertura (por reserva de mesa ou por cliente), envio de pedidos
 * (com baixa de insumos pela ficha tecnica), cancelamento de item, taxa de servico, desconto,
 * pagamentos (varios, para dividir a conta) e fechamento com lancamento no caixa.
 */
@Service
@RequiredArgsConstructor
public class ComandaRestauranteService {

    private final ComandaRestauranteRepository comandaRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaMesaService reservaMesaService;
    private final ItemCardapioRepository itemRepository;
    private final InsumoService insumoService;
    private final PagamentoRepository pagamentoRepository;
    private final AuditoriaService auditoria;

    @Value("${araca-beach.restaurante.taxa-servico-percentual:10}")
    private BigDecimal taxaServicoPadrao;

    // ---------- consulta ----------

    @Transactional(readOnly = true)
    public List<ComandaResponse> listarAbertas() {
        return comandaRepository.findByStatusOrderByAbertaEmAsc(StatusComandaRestaurante.ABERTA).stream()
                .map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public ComandaResponse obter(Long id) {
        return paraResponse(buscar(id));
    }

    // ---------- abertura ----------

    @Transactional
    public ComandaResponse abrir(ComandaAbrirRequest r) {
        ComandaRestaurante comanda;
        if (r.reservaMesaId() != null) {
            ReservaMesa reserva = reservaMesaService.buscar(r.reservaMesaId());
            if (reserva.getStatus() != StatusReservaMesa.CONFIRMADA) {
                throw new IllegalArgumentException("Esta reserva não está mais confirmada (status: " + reserva.getStatus() + ").");
            }
            if (comandaRepository.findFirstByReservaMesaIdAndStatusNot(reserva.getId(), StatusComandaRestaurante.CANCELADA).isPresent()) {
                throw new IllegalArgumentException("Esta reserva já tem uma comanda.");
            }
            String mesa = limpar(r.mesa()) != null ? limpar(r.mesa()) : reserva.getMesa();
            if (mesa == null) {
                throw new IllegalArgumentException("Informe a mesa para atender esta reserva.");
            }
            reserva.setMesa(mesa);
            reserva.setStatus(StatusReservaMesa.EM_ATENDIMENTO);
            comanda = ComandaRestaurante.builder()
                    .cliente(reserva.getCliente())
                    .reservaMesa(reserva)
                    .mesa(mesa)
                    .taxaServicoPercentual(taxaServicoPadrao)
                    .build();
        } else if (Boolean.TRUE.equals(r.avulsa())) {
            comanda = ComandaRestaurante.builder()
                    .nomeAvulso(limpar(r.nome()))
                    .mesa(limpar(r.mesa()))
                    .taxaServicoPercentual(taxaServicoPadrao)
                    .build();
        } else {
            if (r.clienteId() == null) {
                throw new IllegalArgumentException("Informe o cliente (ou uma reserva de mesa) para abrir a comanda.");
            }
            Cliente cliente = clienteRepository.findById(r.clienteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + r.clienteId()));
            comanda = ComandaRestaurante.builder()
                    .cliente(cliente)
                    .mesa(limpar(r.mesa()))
                    .taxaServicoPercentual(taxaServicoPadrao)
                    .build();
        }
        return paraResponse(comandaRepository.save(comanda));
    }

    @Transactional
    public ComandaResponse transferirMesa(Long id, String mesa) {
        ComandaRestaurante c = abertaOuErro(id);
        String nova = limpar(mesa);
        if (c.getReservaMesa() != null && nova == null) {
            throw new IllegalArgumentException("Uma comanda de reserva precisa de uma mesa.");
        }
        auditoria.detalhe("Comanda #" + id + ": mesa '" + c.getMesa() + "' -> '" + nova + "'");
        c.setMesa(nova);
        if (c.getReservaMesa() != null) c.getReservaMesa().setMesa(nova);
        return paraResponse(comandaRepository.save(c));
    }

    // ---------- pedidos ----------

    @Transactional
    public ComandaResponse lancarPedido(Long id, PedidoRequest request) {
        ComandaRestaurante c = abertaOuErro(id);
        int numero = c.getPedidos().stream().mapToInt(PedidoRestaurante::getNumero).max().orElse(0) + 1;
        PedidoRestaurante pedido = PedidoRestaurante.builder().comanda(c).numero(numero).build();

        for (PedidoItemRequest linha : request.itens()) {
            ItemCardapio item = itemRepository.findById(linha.itemId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Item do cardápio não encontrado: " + linha.itemId()));
            if (!item.isAtivo() || !item.getCategoria().isAtiva()) {
                throw new IllegalArgumentException("O item '" + item.getNome() + "' não está mais no cardápio.");
            }
            if (item.isPausado()) {
                throw new IllegalArgumentException("O item '" + item.getNome() + "' está indisponível (acabou).");
            }
            pedido.getItens().add(ItemPedidoRestaurante.builder()
                    .pedido(pedido)
                    .item(item)
                    .nome(item.getNome())
                    .precoUnitario(item.getPreco())
                    .quantidade(linha.quantidade())
                    .observacao(limpar(linha.observacao()))
                    .praca(item.getPraca())
                    .build());
        }
        c.getPedidos().add(pedido);
        comandaRepository.save(c);
        // baixa de insumos (ids ja existem: a comanda foi persistida antes)
        pedido.getItens().forEach(i -> insumoService.consumir(i.getItem(), i.getQuantidade(), c.getId()));
        return paraResponse(c);
    }

    @Transactional
    public ComandaResponse cancelarItem(Long comandaId, Long itemPedidoId, String motivo) {
        ComandaRestaurante c = abertaOuErro(comandaId);
        ItemPedidoRestaurante alvo = c.getPedidos().stream()
                .flatMap(p -> p.getItens().stream())
                .filter(i -> i.getId().equals(itemPedidoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do pedido não encontrado: " + itemPedidoId));
        if (alvo.isCancelado()) {
            throw new IllegalArgumentException("Este item já foi cancelado.");
        }
        alvo.setCancelado(true);
        alvo.setMotivoCancelamento(limpar(motivo));
        insumoService.estornar(alvo.getItem(), alvo.getQuantidade(), comandaId);
        auditoria.detalhe("Comanda #" + comandaId + ": cancelou " + alvo.getQuantidade() + "x " + alvo.getNome()
                + " (R$ " + alvo.getSubtotal() + ")" + (alvo.getMotivoCancelamento() != null ? " - " + alvo.getMotivoCancelamento() : ""));
        return paraResponse(comandaRepository.save(c));
    }

    // ---------- conta ----------

    @Transactional
    public ComandaResponse definirTaxaServico(Long id, BigDecimal percentual) {
        ComandaRestaurante c = abertaOuErro(id);
        c.setTaxaServicoPercentual(percentual);
        auditoria.detalhe("Comanda #" + id + ": taxa de serviço " + percentual + "%");
        return paraResponse(comandaRepository.save(c));
    }

    @Transactional
    public ComandaResponse aplicarDesconto(Long id, DescontoRequest r) {
        ComandaRestaurante c = abertaOuErro(id);
        BigDecimal subtotal = subtotal(c);
        if (r.valor().compareTo(subtotal.add(taxa(c, subtotal))) > 0) {
            throw new IllegalArgumentException("O desconto não pode ser maior que o total da conta.");
        }
        c.setDescontoValor(r.valor());
        c.setDescontoMotivo(limpar(r.motivo()));
        auditoria.detalhe("Comanda #" + id + ": desconto de R$ " + r.valor()
                + (c.getDescontoMotivo() != null ? " - " + c.getDescontoMotivo() : ""));
        return paraResponse(comandaRepository.save(c));
    }

    @Transactional
    public ComandaResponse registrarPagamento(Long id, PagamentoRestRequest r) {
        ComandaRestaurante c = abertaOuErro(id);
        BigDecimal restante = total(c).subtract(totalPago(c));
        if (r.valor().compareTo(restante) > 0) {
            throw new IllegalArgumentException("O valor é maior que o restante da conta (R$ " + restante + ").");
        }
        c.getPagamentos().add(PagamentoComandaRestaurante.builder()
                .comanda(c).valor(r.valor()).formaPagamento(r.forma()).build());
        return paraResponse(comandaRepository.save(c));
    }

    @Transactional
    public ComandaResponse removerPagamento(Long id, Long pagamentoId) {
        ComandaRestaurante c = abertaOuErro(id);
        boolean removido = c.getPagamentos().removeIf(p -> p.getId().equals(pagamentoId));
        if (!removido) throw new RecursoNaoEncontradoException("Pagamento não encontrado: " + pagamentoId);
        auditoria.detalhe("Comanda #" + id + ": removeu pagamento #" + pagamentoId);
        return paraResponse(comandaRepository.save(c));
    }

    @Transactional
    public ComandaResponse fechar(Long id) {
        ComandaRestaurante c = abertaOuErro(id);
        if (itensAtivos(c).isEmpty()) {
            throw new IllegalArgumentException("A comanda não tem itens: cancele-a em vez de fechar.");
        }
        BigDecimal total = total(c);
        BigDecimal pago = totalPago(c);
        int cmp = total.compareTo(pago);
        if (cmp > 0) {
            throw new IllegalArgumentException("Ainda restam R$ " + total.subtract(pago) + " a receber.");
        }
        if (cmp < 0) {
            throw new IllegalArgumentException("Os pagamentos (R$ " + pago + ") excedem o total (R$ " + total + "). Ajuste os pagamentos.");
        }
        // Cada pagamento entra no caixa do dia (sem reserva de quadra), como mensalidades e pacotes.
        c.getPagamentos().forEach(p -> pagamentoRepository.save(Pagamento.builder()
                .reserva(null)
                .valor(p.getValor())
                .formaPagamento(p.getFormaPagamento())
                .status(StatusPagamento.PAGO)
                .ehSinal(false)
                .build()));
        c.setStatus(StatusComandaRestaurante.FECHADA);
        c.setFechadaEm(LocalDateTime.now());
        if (c.getReservaMesa() != null) c.getReservaMesa().setStatus(StatusReservaMesa.CONCLUIDA);
        auditoria.detalhe("Comanda #" + id + " fechada: R$ " + total + " (" + c.getPagamentos().size() + " pagamento(s))");
        return paraResponse(comandaRepository.save(c));
    }

    @Transactional
    public ComandaResponse cancelar(Long id) {
        ComandaRestaurante c = abertaOuErro(id);
        if (!itensAtivos(c).isEmpty()) {
            throw new IllegalArgumentException("Cancele os itens da comanda antes de cancelá-la.");
        }
        if (!c.getPagamentos().isEmpty()) {
            throw new IllegalArgumentException("Remova os pagamentos antes de cancelar a comanda.");
        }
        c.setStatus(StatusComandaRestaurante.CANCELADA);
        c.setFechadaEm(LocalDateTime.now());
        if (c.getReservaMesa() != null) c.getReservaMesa().setStatus(StatusReservaMesa.CONFIRMADA);
        auditoria.detalhe("Comanda #" + id + " cancelada");
        return paraResponse(comandaRepository.save(c));
    }

    // ---------- calculo (publico para o relatorio) ----------

    public static List<ItemPedidoRestaurante> itensAtivos(ComandaRestaurante c) {
        return c.getPedidos().stream().flatMap(p -> p.getItens().stream()).filter(i -> !i.isCancelado()).toList();
    }

    public static BigDecimal subtotal(ComandaRestaurante c) {
        return itensAtivos(c).stream().map(ItemPedidoRestaurante::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal taxa(ComandaRestaurante c, BigDecimal subtotal) {
        return subtotal.multiply(c.getTaxaServicoPercentual()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal total(ComandaRestaurante c) {
        BigDecimal sub = subtotal(c);
        return sub.add(taxa(c, sub)).subtract(c.getDescontoValor()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalPago(ComandaRestaurante c) {
        return c.getPagamentos().stream().map(PagamentoComandaRestaurante::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- internos ----------

    private ComandaRestaurante buscar(Long id) {
        return comandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda não encontrada: " + id));
    }

    private ComandaRestaurante abertaOuErro(Long id) {
        ComandaRestaurante c = buscar(id);
        if (c.getStatus() != StatusComandaRestaurante.ABERTA) {
            throw new IllegalArgumentException("Esta comanda já foi encerrada.");
        }
        return c;
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public ComandaResponse paraResponse(ComandaRestaurante c) {
        BigDecimal subtotal = subtotal(c);
        BigDecimal taxa = taxa(c, subtotal);
        BigDecimal total = total(c);
        BigDecimal pago = totalPago(c);

        String rotulo;
        if (c.getCliente() == null) {
            String base = c.getNomeAvulso() != null ? c.getNomeAvulso() : "Venda avulsa #" + c.getId();
            rotulo = base + (c.getMesa() != null ? " · Mesa " + c.getMesa() : "");
        } else if (c.getReservaMesa() != null) {
            rotulo = "Mesa " + c.getMesa() + " · " + c.getCliente().getNome();
        } else {
            rotulo = c.getCliente().getNome() + (c.getMesa() != null ? " · Mesa " + c.getMesa() : "");
        }

        List<PedidoResponse> pedidos = c.getPedidos().stream().map(p -> new PedidoResponse(
                p.getId(), p.getNumero(), p.getCriadoEm(),
                p.getItens().stream().map(i -> new ItemPedidoResponse(i.getId(), i.getItem().getId(), i.getNome(),
                        i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal(), i.getObservacao(), i.getPraca(),
                        i.isCancelado(), i.getMotivoCancelamento())).toList())).toList();

        List<PagamentoRestResponse> pagamentos = c.getPagamentos().stream()
                .map(p -> new PagamentoRestResponse(p.getId(), p.getValor(), p.getFormaPagamento(), p.getCriadoEm())).toList();

        return new ComandaResponse(c.getId(), c.getStatus(),
                c.getCliente() != null ? c.getCliente().getId() : null,
                c.getCliente() != null ? c.getCliente().getNome() : (c.getNomeAvulso() != null ? c.getNomeAvulso() : "Venda avulsa"),
                c.getReservaMesa() != null ? c.getReservaMesa().getId() : null, c.getMesa(),
                c.getCliente() == null ? "AVULSA" : (c.getReservaMesa() != null ? "MESA" : "CLIENTE"), rotulo, c.getAbertaEm(), c.getFechadaEm(),
                c.getTaxaServicoPercentual(), subtotal, taxa, c.getDescontoValor(), c.getDescontoMotivo(),
                total, pago, total.subtract(pago), taxaServicoPadrao, pedidos, pagamentos);
    }
}
