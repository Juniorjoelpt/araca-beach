package com.aracabeach.service;

import com.aracabeach.config.OperadorAtual;
import com.aracabeach.domain.dayuse.*;
import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.financeiro.FormaPagamento;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.dto.DayUseDtos.*;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Day Use: ingresso diario de acesso a arena (so entrada), vendido no balcao com check-in na portaria. */
@Service
@RequiredArgsConstructor
public class DayUseService {

    private static final long CONFIG_ID = 1L;

    private final DayUseTipoRepository tipoRepository;
    private final DayUseVendaRepository vendaRepository;
    private final DayUseConfigRepository configRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AuditoriaService auditoria;

    // ---------- tipos e configuracao ----------

    static int pessoas(Integer porUnidade) {
        return porUnidade == null || porUnidade < 1 ? 1 : porUnidade;
    }

    static boolean fimDeSemana(LocalDate data) {
        return data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    static BigDecimal precoNaData(DayUseTipo t, LocalDate data) {
        return fimDeSemana(data) && t.getPrecoFimSemana() != null && t.getPrecoFimSemana().signum() > 0 ? t.getPrecoFimSemana() : t.getPrecoSemana();
    }

    private TipoResponse paraResponse(DayUseTipo t, LocalDate data) {
        return new TipoResponse(t.getId(), t.getNome(), t.getPrecoSemana(), t.getPrecoFimSemana(), precoNaData(t, data),
                pessoas(t.getPessoasPorUnidade()), t.getCusto(), t.isAtivo());
    }

    @Transactional(readOnly = true)
    public List<TipoResponse> tiposAtivos(LocalDate data) {
        return tipoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(t -> paraResponse(t, data)).toList();
    }

    @Transactional(readOnly = true)
    public List<TipoResponse> todosOsTipos() {
        LocalDate hoje = LocalDate.now();
        return tipoRepository.findAllByOrderByNomeAsc().stream().map(t -> paraResponse(t, hoje)).toList();
    }

    @Transactional
    public TipoResponse criarTipo(TipoRequest r) {
        DayUseTipo t = DayUseTipo.builder().nome(r.nome().trim()).precoSemana(r.precoSemana())
                .precoFimSemana(r.precoFimSemana()).pessoasPorUnidade(pessoas(r.pessoasPorUnidade())).custo(r.custo())
                .ativo(r.ativo() == null || r.ativo()).build();
        auditoria.detalhe("Day Use: novo ingresso '" + t.getNome() + "'");
        return paraResponse(tipoRepository.save(t), LocalDate.now());
    }

    @Transactional
    public TipoResponse atualizarTipo(Long id, TipoRequest r) {
        DayUseTipo t = tipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de ingresso não encontrado: " + id));
        t.setNome(r.nome().trim());
        t.setPrecoSemana(r.precoSemana());
        t.setPrecoFimSemana(r.precoFimSemana());
        t.setPessoasPorUnidade(pessoas(r.pessoasPorUnidade()));
        t.setCusto(r.custo());
        if (r.ativo() != null) t.setAtivo(r.ativo());
        auditoria.detalhe("Day Use: ingresso '" + t.getNome() + "' atualizado");
        return paraResponse(tipoRepository.save(t), LocalDate.now());
    }

    /** Exclui o tipo. As vendas ja feitas guardam nome e preco proprios, entao nao sao afetadas. */
    @Transactional
    public void excluirTipo(Long id) {
        DayUseTipo t = tipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de ingresso não encontrado: " + id));
        auditoria.detalhe("Day Use: ingresso '" + t.getNome() + "' excluído");
        tipoRepository.delete(t);
    }

    @Transactional(readOnly = true)
    public ConfigResponse config() {
        DayUseConfig c = configRepository.findById(CONFIG_ID).orElse(null);
        return new ConfigResponse(c == null || c.getCapacidadeDiaria() == null ? 0 : c.getCapacidadeDiaria(),
                c == null ? null : c.getMensagemPortal());
    }

    @Transactional
    public ConfigResponse salvarConfig(ConfigRequest r) {
        DayUseConfig c = configRepository.findById(CONFIG_ID).orElse(DayUseConfig.builder().id(CONFIG_ID).build());
        c.setCapacidadeDiaria(r.capacidadeDiaria() == null ? 0 : r.capacidadeDiaria());
        c.setMensagemPortal(limpar(r.mensagemPortal()));
        configRepository.save(c);
        auditoria.detalhe("Day Use: capacidade diária = " + c.getCapacidadeDiaria());
        return config();
    }

    // ---------- vendas ----------

    private static final Set<StatusDayUse> OCUPAM_VAGA = Set.of(StatusDayUse.ATIVA, StatusDayUse.PENDENTE);

    @Transactional(readOnly = true)
    public DiaResponse dia(LocalDate data) {
        List<DayUseVenda> vendas = vendaRepository.findByDataOrderByCriadoEmDesc(data);
        int vendidas = 0, reservadas = 0, entraram = 0, canceladas = 0, qtdVendas = 0;
        BigDecimal valor = BigDecimal.ZERO;
        for (DayUseVenda v : vendas) {
            switch (v.getStatus()) {
                case ATIVA -> {
                    vendidas += v.getPessoas();
                    entraram += v.getEntraram();
                    valor = valor.add(v.getValorTotal());
                    qtdVendas++;
                }
                case PENDENTE -> reservadas += v.getPessoas();
                default -> canceladas++;
            }
        }
        Integer capacidade = capacidade();
        Integer vagas = capacidade == null ? null : Math.max(0, capacidade - vendidas - reservadas);
        ResumoDia resumo = new ResumoDia(data, vendidas, reservadas, entraram, capacidade, vagas, valor, qtdVendas, canceladas);
        return new DiaResponse(resumo, vendas.stream().map(this::paraResponse).toList());
    }

    /** Tipos e vagas de um dia (portal do cliente). */
    @Transactional(readOnly = true)
    public DisponibilidadeResponse disponibilidade(LocalDate data) {
        Integer capacidade = capacidade();
        Integer vagas = capacidade == null ? null : Math.max(0, capacidade - ocupadas(data));
        return new DisponibilidadeResponse(data, capacidade, vagas, config().mensagemPortal(), tiposAtivos(data));
    }

    /** Venda de balcao: paga na hora e ja entra no caixa. */
    @Transactional
    public VendaResponse vender(VendaRequest r) {
        if (r.data().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível vender Day Use para uma data que já passou.");
        }
        DayUseVenda venda = montar(r.data(), r.itens(), "BALCAO");
        venda.setNomeCliente(limpar(r.nomeCliente()));
        venda.setTelefone(limpar(r.telefone()));
        venda.setObservacao(limpar(r.observacao()));
        venda.setOperador(OperadorAtual.login());
        venda = vendaRepository.save(venda);
        receber(venda, r.forma());
        auditoria.detalhe("Day Use #" + venda.getId() + ": " + venda.getPessoas() + " pessoa(s) para " + r.data() + ", total " + venda.getValorTotal());
        return paraResponse(venda);
    }

    /** Reserva online do cliente do portal: ocupa vagas, fica PENDENTE ate a recepcao confirmar o pagamento. */
    @Transactional
    public VendaResponse reservarPeloPortal(Cliente cliente, PortalDayUseRequest r) {
        LocalDate hoje = LocalDate.now();
        if (r.data().isBefore(hoje)) throw new IllegalArgumentException("Escolha uma data a partir de hoje.");
        if (r.data().isAfter(hoje.plusDays(90))) throw new IllegalArgumentException("Só é possível reservar com até 90 dias de antecedência.");
        if (vendaRepository.countByClienteIdAndStatus(cliente.getId(), StatusDayUse.PENDENTE) >= 5) {
            throw new IllegalArgumentException("Você já tem 5 reservas de Day Use aguardando pagamento. Pague ou cancele alguma antes de reservar outra.");
        }
        DayUseVenda venda = montar(r.data(), r.itens(), "PORTAL");
        venda.setStatus(StatusDayUse.PENDENTE);
        venda.setClienteId(cliente.getId());
        venda.setNomeCliente(limpar(cliente.getNome()));
        venda.setTelefone(limpar(cliente.getTelefone()));
        venda.setObservacao(limpar(r.observacao()));
        venda = vendaRepository.save(venda);
        auditoria.detalhe("Day Use #" + venda.getId() + ": reserva online de " + venda.getPessoas() + " pessoa(s) para " + r.data());
        return paraResponse(venda);
    }

    @Transactional(readOnly = true)
    public List<VendaResponse> minhasReservas(Cliente cliente) {
        return vendaRepository.findByClienteIdOrderByDataDescCriadoEmDesc(cliente.getId()).stream().map(this::paraResponse).toList();
    }

    @Transactional
    public VendaResponse cancelarPeloPortal(Cliente cliente, Long id) {
        DayUseVenda v = buscar(id);
        if (v.getClienteId() == null || !v.getClienteId().equals(cliente.getId())) {
            throw new RecursoNaoEncontradoException("Venda de Day Use não encontrada: " + id);
        }
        if (v.getStatus() != StatusDayUse.PENDENTE) {
            throw new IllegalArgumentException("Só é possível cancelar reservas que ainda aguardam pagamento. Fale com a recepção.");
        }
        v.setStatus(StatusDayUse.CANCELADA);
        v.setCanceladaEm(LocalDateTime.now());
        v.setMotivoCancelamento("Cancelada pelo cliente");
        return paraResponse(vendaRepository.save(v));
    }

    /** A recepcao recebeu o pagamento de uma reserva online: vira venda ativa e entra no caixa. */
    @Transactional
    public VendaResponse confirmarPagamento(Long id, FormaPagamento forma) {
        DayUseVenda v = buscar(id);
        if (v.getStatus() != StatusDayUse.PENDENTE) throw new IllegalArgumentException("Esta reserva não está aguardando pagamento.");
        if (v.getData().isBefore(LocalDate.now())) throw new IllegalArgumentException("Esta reserva é de uma data que já passou.");
        v.setOperador(OperadorAtual.login());
        receber(v, forma);
        auditoria.detalhe("Day Use #" + id + ": pagamento confirmado (" + forma + "), total " + v.getValorTotal());
        return paraResponse(v);
    }

    @Transactional
    public VendaResponse registrarEntrada(Long id, int quantidade) {
        DayUseVenda v = buscar(id);
        if (v.getStatus() == StatusDayUse.PENDENTE) throw new IllegalArgumentException("Esta reserva ainda não foi paga. Confirme o pagamento primeiro.");
        if (v.getStatus() != StatusDayUse.ATIVA) throw new IllegalArgumentException("Esta venda está cancelada.");
        if (!v.getData().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Este ingresso vale para " + v.getData() + ", não para hoje.");
        }
        if (v.getEntraram() + quantidade > v.getPessoas()) {
            throw new IllegalArgumentException("Só restam " + (v.getPessoas() - v.getEntraram()) + " entrada(s) nesta venda.");
        }
        v.setEntraram(v.getEntraram() + quantidade);
        auditoria.detalhe("Day Use #" + id + ": entrada de " + quantidade + " (" + v.getEntraram() + "/" + v.getPessoas() + ")");
        return paraResponse(vendaRepository.save(v));
    }

    @Transactional
    public VendaResponse cancelar(Long id, String motivo) {
        DayUseVenda v = buscar(id);
        if (v.getStatus() == StatusDayUse.CANCELADA) throw new IllegalArgumentException("Esta venda já está cancelada.");
        if (v.getEntraram() > 0) {
            throw new IllegalArgumentException("Já houve entrada com este ingresso; não é possível cancelar.");
        }
        v.setStatus(StatusDayUse.CANCELADA);
        v.setCanceladaEm(LocalDateTime.now());
        v.setMotivoCancelamento(limpar(motivo));
        // O valor sai do caixa (pagamento estornado), sem apagar o registro.
        if (v.getPagamentoId() != null) {
            pagamentoRepository.findById(v.getPagamentoId()).ifPresent(p -> {
                p.setStatus(StatusPagamento.ESTORNADO);
                pagamentoRepository.save(p);
            });
        }
        auditoria.detalhe("Day Use #" + id + " cancelado" + (v.getMotivoCancelamento() != null ? ": " + v.getMotivoCancelamento() : ""));
        return paraResponse(vendaRepository.save(v));
    }

    // ---------- internos ----------

    /** Monta a venda (itens, totais) e valida a capacidade do dia. Status ATIVA; quem chama ajusta. */
    private DayUseVenda montar(LocalDate data, List<ItemVendaRequest> pedidos, String origem) {
        DayUseVenda venda = DayUseVenda.builder().data(data).origem(origem).valorTotal(BigDecimal.ZERO).build();
        List<DayUseItem> itens = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int pessoas = 0;
        for (ItemVendaRequest i : pedidos) {
            DayUseTipo tipo = tipoRepository.findById(i.tipoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de ingresso não encontrado: " + i.tipoId()));
            if (!tipo.isAtivo()) throw new IllegalArgumentException("O ingresso '" + tipo.getNome() + "' não está disponível.");
            BigDecimal preco = precoNaData(tipo, data);
            int porUnidade = pessoas(tipo.getPessoasPorUnidade());
            itens.add(DayUseItem.builder().venda(venda).tipoId(tipo.getId()).tipoNome(tipo.getNome())
                    .precoUnitario(preco).quantidade(i.quantidade()).pessoasPorUnidade(porUnidade)
                    .custoUnitario(tipo.getCusto()).build());
            total = total.add(preco.multiply(BigDecimal.valueOf(i.quantidade())));
            pessoas += i.quantidade() * porUnidade;
        }
        Integer capacidade = capacidade();
        if (capacidade != null) {
            int ja = ocupadas(data);
            if (ja + pessoas > capacidade) {
                throw new IllegalArgumentException("Capacidade do dia excedida: restam " + Math.max(0, capacidade - ja)
                        + " vaga(s) para " + data + ".");
            }
        }
        venda.setItens(itens);
        venda.setPessoas(pessoas);
        venda.setValorTotal(total);
        return venda;
    }

    /** Marca como paga e lanca no caixa (data do caixa = momento do pagamento). */
    private void receber(DayUseVenda venda, FormaPagamento forma) {
        venda.setStatus(StatusDayUse.ATIVA);
        venda.setFormaPagamento(forma);
        venda.setPagoEm(LocalDateTime.now());
        if (venda.getValorTotal().signum() > 0) {
            Pagamento pg = pagamentoRepository.save(Pagamento.builder()
                    .reserva(null)
                    .valor(venda.getValorTotal())
                    .formaPagamento(forma)
                    .status(StatusPagamento.PAGO)
                    .ehSinal(false)
                    .origem("DAYUSE")
                    .descricao("Day Use #" + venda.getId() + " - " + venda.getPessoas() + " pessoa(s) - " + venda.getData()
                            + (venda.getNomeCliente() != null ? " - " + venda.getNomeCliente() : ""))
                    .operador(venda.getOperador())
                    .build());
            venda.setPagamentoId(pg.getId());
        }
        vendaRepository.save(venda);
    }

    private int ocupadas(LocalDate data) {
        return vendaRepository.findByDataAndStatusIn(data, OCUPAM_VAGA).stream().mapToInt(DayUseVenda::getPessoas).sum();
    }

    private Integer capacidade() {
        Integer c = configRepository.findById(CONFIG_ID).map(DayUseConfig::getCapacidadeDiaria).orElse(0);
        return c == null || c <= 0 ? null : c;
    }

    private DayUseVenda buscar(Long id) {
        return vendaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Venda de Day Use não encontrada: " + id));
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private VendaResponse paraResponse(DayUseVenda v) {
        return new VendaResponse(v.getId(), v.getData(), v.getNomeCliente(), v.getTelefone(), v.getObservacao(),
                v.getFormaPagamento(), v.getValorTotal(), v.getPessoas(), v.getEntraram(), v.getStatus(),
                v.getOrigem() == null ? "BALCAO" : v.getOrigem(), v.getOperador(), v.getCriadoEm(), v.getPagoEm(),
                v.getCanceladaEm(), v.getMotivoCancelamento(),
                v.getItens().stream().map(i -> {
                    int porUnidade = pessoas(i.getPessoasPorUnidade());
                    return new ItemVendaResponse(i.getTipoId(), i.getTipoNome(), i.getPrecoUnitario(), i.getQuantidade(),
                            porUnidade, i.getQuantidade() * porUnidade,
                            i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())));
                }).toList());
    }
}
