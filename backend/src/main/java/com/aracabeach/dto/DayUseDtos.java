package com.aracabeach.dto;

import com.aracabeach.domain.dayuse.StatusDayUse;
import com.aracabeach.domain.financeiro.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DayUseDtos {

    private DayUseDtos() {}

    // ---------- requests ----------

    /** pessoasPorUnidade > 1 = pacote para grupo (ex.: 10 pessoas por um preco fechado). */
    public record TipoRequest(@NotBlank @Size(max = 80) String nome,
                              @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precoSemana,
                              @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precoFimSemana,
                              @Min(1) @Max(500) Integer pessoasPorUnidade,
                              @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal custo,
                              Boolean ativo) {}

    public record ConfigRequest(@Min(0) @Max(100000) Integer capacidadeDiaria,
                                @Size(max = 300) String mensagemPortal) {}

    public record ItemVendaRequest(@NotNull Long tipoId, @Min(1) @Max(200) int quantidade) {}

    public record VendaRequest(@NotNull LocalDate data,
                               @Size(max = 80) String nomeCliente,
                               @Size(max = 30) String telefone,
                               @Size(max = 200) String observacao,
                               @NotNull FormaPagamento forma,
                               @NotEmpty @Valid List<ItemVendaRequest> itens) {}

    public record PortalDayUseRequest(@NotNull LocalDate data,
                                       @Size(max = 200) String observacao,
                                       @NotEmpty @Valid List<ItemVendaRequest> itens) {}

    public record EntradaRequest(@Min(1) @Max(500) int quantidade) {}

    public record ConfirmarPagamentoRequest(@NotNull FormaPagamento forma) {}

    // ---------- responses ----------

    /** `preco` ja e o valor valido para a data consultada (semana ou fim de semana). */
    public record TipoResponse(Long id, String nome, BigDecimal precoSemana, BigDecimal precoFimSemana,
                               BigDecimal preco, int pessoasPorUnidade, BigDecimal custo, boolean ativo) {}

    public record ConfigResponse(Integer capacidadeDiaria, String mensagemPortal) {}

    public record ItemVendaResponse(Long tipoId, String tipoNome, BigDecimal precoUnitario, int quantidade,
                                    int pessoasPorUnidade, int pessoas, BigDecimal subtotal) {}

    public record VendaResponse(Long id, LocalDate data, String nomeCliente, String telefone, String observacao,
                                FormaPagamento forma, BigDecimal valorTotal, int pessoas, int entraram,
                                StatusDayUse status, String origem, String operador, LocalDateTime criadoEm,
                                LocalDateTime pagoEm, LocalDateTime canceladaEm, String motivoCancelamento,
                                List<ItemVendaResponse> itens) {}

    public record ResumoDia(LocalDate data, int pessoasVendidas, int pessoasReservadas, int pessoasEntraram,
                            Integer capacidade, Integer vagasRestantes, BigDecimal valorTotal, int vendas, int canceladas) {}

    public record DiaResponse(ResumoDia resumo, List<VendaResponse> vendas) {}

    /** Disponibilidade de um dia para o portal do cliente. */
    public record DisponibilidadeResponse(LocalDate data, Integer capacidade, Integer vagasRestantes, String mensagemPortal,
                                          List<TipoResponse> tipos) {}
}
