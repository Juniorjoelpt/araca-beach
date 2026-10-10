package com.aracabeach.dto;

import com.aracabeach.domain.financeiro.FormaPagamento;
import com.aracabeach.domain.restaurante.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs do modulo Restaurante (requests e responses agrupados). */
public final class RestauranteDtos {

    private RestauranteDtos() {}

    // ---------------- requests ----------------

    public record CategoriaRequest(@NotBlank @Size(max = 80) String nome, Integer ordem, Boolean ativa) {}

    public record ItemCardapioRequest(
            @NotNull Long categoriaId,
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 500) String descricao,
            @Size(max = 80) String porcao,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal preco,
            Praca praca,
            Boolean ativo,
            Boolean pausado,
            Integer tempoPreparoMin,
            Integer ordem,
            @Size(max = 30) @Pattern(regexp = "^[0-9A-Za-z\\-\\s]*$", message = "Código de barras inválido") String codigoBarras) {}

    public record PausaRequest(@NotNull Boolean pausado) {}

    public record FichaLinhaRequest(@NotNull Long insumoId, @NotNull @DecimalMin(value = "0.0001") BigDecimal quantidade) {}

    /** Ficha tecnica: insumos por unidade vendida + custo adicional de producao (opcional). */
    public record FichaRequest(@NotNull @Valid List<FichaLinhaRequest> linhas,
                               @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal custoProducao) {}

    public record InsumoRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotNull UnidadeInsumo unidade,
            @DecimalMin("0.0") BigDecimal estoqueMinimo,
            @DecimalMin("0.0") BigDecimal custoUnitario,
            @DecimalMin("0.0") BigDecimal unidadesPorEmbalagem,
            Boolean ativo) {}

    public record MovimentoInsumoRequest(
            @NotNull TipoMovimentoInsumo tipo,
            @NotNull @DecimalMin("0.0") BigDecimal quantidade,
            @DecimalMin("0.0") BigDecimal custoUnitario,
            @Size(max = 200) String observacao) {}

    public record ReservaMesaRequest(
            @NotNull Long clienteId,
            @NotNull LocalDateTime dataHora,
            @Min(1) int pessoas,
            @Size(max = 40) String mesa,
            @Size(max = 300) String observacoes) {}

    public record MesaRequest(@Size(max = 40) String mesa) {}

    public record ComandaAbrirRequest(Long reservaMesaId, Long clienteId, @Size(max = 40) String mesa,
                                      Boolean avulsa, @Size(max = 80) String nome,
                                      Boolean cortesia, @Size(max = 200) String motivo) {}

    public record PedidoItemRequest(@NotNull Long itemId, @Min(1) @Max(99) int quantidade, @Size(max = 200) String observacao) {}

    public record PedidoRequest(@NotEmpty @Valid List<PedidoItemRequest> itens) {}

    public record TaxaServicoRequest(@NotNull @DecimalMin("0.0") @DecimalMax("30.0") BigDecimal percentual) {}

    public record PagamentoRestRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal valor, @NotNull FormaPagamento forma) {}

    public record DescontoRequest(@NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal valor, @Size(max = 200) String motivo) {}

    // ---------------- responses ----------------

    public record CardapioItemResponse(Long id, String nome, String descricao, String porcao, BigDecimal preco,
                                       Praca praca, boolean pausado, Integer tempoPreparoMin,
                                       String codigoBarras) {}

    public record CardapioCategoriaResponse(Long id, String nome, int ordem, List<CardapioItemResponse> itens) {}

    public record CategoriaResponse(Long id, String nome, int ordem, boolean ativa) {}

    public record FichaLinhaResponse(Long insumoId, String insumoNome, UnidadeInsumo unidade, BigDecimal quantidade,
                                     BigDecimal custoLinha) {}

    public record ItemGestaoResponse(Long id, Long categoriaId, String categoriaNome, String nome, String descricao,
                                     String porcao, BigDecimal preco, Praca praca, boolean ativo, boolean pausado,
                                     Integer tempoPreparoMin, int ordem, BigDecimal custo, BigDecimal margem,
                                     BigDecimal margemPercentual, boolean temFicha, List<FichaLinhaResponse> ficha,
                                     String codigoBarras, BigDecimal custoProducao) {}

    public record InsumoResponse(Long id, String nome, UnidadeInsumo unidade, BigDecimal estoqueAtual,
                                 BigDecimal estoqueMinimo, BigDecimal custoUnitario, boolean ativo, boolean abaixoDoMinimo,
                                 BigDecimal unidadesPorEmbalagem) {}

    public record MovimentoInsumoResponse(Long id, TipoMovimentoInsumo tipo, BigDecimal quantidade, String observacao,
                                          Long comandaId, LocalDateTime criadoEm) {}

    public record ReservaMesaResponse(Long id, Long clienteId, String clienteNome, String clienteTelefone,
                                      LocalDateTime dataHora, int pessoas, String mesa, StatusReservaMesa status,
                                      String observacoes, Long comandaId) {}

    public record ItemPedidoResponse(Long id, Long itemId, String nome, int quantidade, BigDecimal precoUnitario,
                                     BigDecimal subtotal, String observacao, Praca praca, boolean cancelado,
                                     String motivoCancelamento) {}

    public record PedidoResponse(Long id, int numero, LocalDateTime criadoEm, List<ItemPedidoResponse> itens, String lancadoPor) {}

    public record PedidoParaImprimir(ComandaResponse comanda, PedidoResponse pedido) {}

    public record PagamentoRestResponse(Long id, BigDecimal valor, FormaPagamento forma, LocalDateTime criadoEm) {}

    public record ComandaResponse(Long id, StatusComandaRestaurante status, Long clienteId, String clienteNome,
                                  Long reservaMesaId, String mesa, String vinculo, String rotulo,
                                  LocalDateTime abertaEm, LocalDateTime fechadaEm, BigDecimal taxaServicoPercentual,
                                  BigDecimal subtotal, BigDecimal taxaServico, BigDecimal desconto, String descontoMotivo,
                                  BigDecimal total, BigDecimal totalPago, BigDecimal restante,
                                  BigDecimal taxaServicoPadrao, List<PedidoResponse> pedidos, List<PagamentoRestResponse> pagamentos,
                                  boolean cortesia, String cortesiaMotivo, String abertaPor) {}

    public record ItemVendidoResponse(Long itemId, String nome, long quantidade, BigDecimal receita,
                                      BigDecimal custoTotal, BigDecimal margem) {}

    public record FaixaResponse(int chave, long quantidade, BigDecimal receita) {}

    public record RelatorioRestauranteResponse(java.time.LocalDate inicio, java.time.LocalDate fim,
                                               int comandas, BigDecimal receitaItens, BigDecimal taxaServico,
                                               BigDecimal descontos, BigDecimal receitaTotal, BigDecimal ticketMedio,
                                               BigDecimal custoTotal, BigDecimal margemTotal, int itensSemFicha,
                                               List<ItemVendidoResponse> maisVendidos,
                                               List<FaixaResponse> porHora, List<FaixaResponse> porDiaSemana,
                                               List<String> insumosAbaixoDoMinimo,
                                               int cortesias, BigDecimal valorCortesias, BigDecimal custoCortesias,
                                               List<String> itensSemCusto, BigDecimal lucroAposCortesias) {}
}
