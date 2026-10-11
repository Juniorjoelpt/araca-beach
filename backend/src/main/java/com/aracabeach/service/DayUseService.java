package com.aracabeach.service;

import com.aracabeach.config.OperadorAtual;
import com.aracabeach.domain.dayuse.*;
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

    static boolean fimDeSemana(LocalDate data) {
        return data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    static BigDecimal precoNaData(DayUseTipo t, LocalDate data) {
        return fimDeSemana(data) && t.getPrecoFimSemana() != null ? t.getPrecoFimSemana() : t.getPrecoSemana();
    }

    private TipoResponse paraResponse(DayUseTipo t, LocalDate data) {
        return new TipoResponse(t.getId(), t.getNome(), t.getPrecoSemana(), t.getPrecoFimSemana(), precoNaData(t, data), t.isAtivo());
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
                .precoFimSemana(r.precoFimSemana()).ativo(r.ativo() == null || r.ativo()).build();
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
        if (r.ativo() != null) t.setAtivo(r.ativo());
        auditoria.detalhe("Day Use: ingresso '" + t.getNome() + "' atualizado");
        return paraResponse(tipoRepository.save(t), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public ConfigResponse config() {
        return new ConfigResponse(configRepository.findById(CONFIG_ID).map(DayUseConfig::getCapacidadeDiaria).orElse(0));
    }

    @Transactional
    public ConfigResponse salvarConfig(ConfigRequest r) {
        DayUseConfig c = configRepository.findById(CONFIG_ID).orElse(DayUseConfig.builder().id(CONFIG_ID).build());
        c.setCapacidadeDiaria(r.capacidadeDiaria() == null ? 0 : r.capacidadeDiaria());
        configRepository.save(c);
        auditoria.detalhe("Day Use: capacidade diária = " + c.getCapacidadeDiaria());
        return new ConfigResponse(c.getCapacidadeDiaria());
    }

    // ---------- vendas ----------

    @Transactional(readOnly = true)
    public DiaResponse dia(LocalDate data) {
        List<DayUseVenda> vendas = vendaRepository.findByDataOrderByCriadoEmDesc(data);
        int vendidas = 0, entraram = 0, canceladas = 0;
        BigDecimal valor = BigDecimal.ZERO;
        for (DayUseVenda v : vendas) {
            if (v.getStatus() == StatusDayUse.ATIVA) {
                vendidas += v.getPessoas();
                entraram += v.getEntraram();
                valor = valor.add(v.getValorTotal());
            } else {
                canceladas++;
            }
        }
        Integer capacidade = capacidade();
        Integer vagas = capacidade == null ? null : Math.max(0, capacidade - vendidas);
        ResumoDia resumo = new ResumoDia(data, vendidas, entraram, capacidade, vagas, valor,
                (int) vendas.stream().filter(v -> v.getStatus() == StatusDayUse.ATIVA).count(), canceladas);
        return new DiaResponse(resumo, vendas.stream().map(this::paraResponse).toList());
    }

    @Transactional
    public VendaResponse vender(VendaRequest r) {
        if (r.data().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível vender Day Use para uma data que já passou.");
        }
        List<DayUseItem> itens = new ArrayList<>();
        DayUseVenda venda = DayUseVenda.builder()
                .data(r.data())
                .nomeCliente(limpar(r.nomeCliente()))
                .telefone(limpar(r.telefone()))
                .observacao(limpar(r.observacao()))
                .formaPagamento(r.forma())
                .operador(OperadorAtual.login())
                .build();
        BigDecimal total = BigDecimal.ZERO;
        int pessoas = 0;
        for (ItemVendaRequest i : r.itens()) {
            DayUseTipo tipo = tipoRepository.findById(i.tipoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de ingresso não encontrado: " + i.tipoId()));
            if (!tipo.isAtivo()) throw new IllegalArgumentException("O ingresso '" + tipo.getNome() + "' não está disponível.");
            BigDecimal preco = precoNaData(tipo, r.data());
            itens.add(DayUseItem.builder().venda(venda).tipoId(tipo.getId()).tipoNome(tipo.getNome())
                    .precoUnitario(preco).quantidade(i.quantidade()).build());
            total = total.add(preco.multiply(BigDecimal.valueOf(i.quantidade())));
            pessoas += i.quantidade();
        }
        Integer capacidade = capacidade();
        if (capacidade != null) {
            int ja = vendaRepository.findByDataAndStatus(r.data(), StatusDayUse.ATIVA).stream().mapToInt(DayUseVenda::getPessoas).sum();
            if (ja + pessoas > capacidade) {
                throw new IllegalArgumentException("Capacidade do dia excedida: restam " + Math.max(0, capacidade - ja)
                        + " vaga(s) para " + r.data() + ".");
            }
        }
        venda.setItens(itens);
        venda.setPessoas(pessoas);
        venda.setValorTotal(total);
        venda = vendaRepository.save(venda);

        // Entra no caixa como os demais recebimentos (a data do caixa e a do pagamento, nao a do dia de uso).
        if (total.signum() > 0) {
            Pagamento pg = pagamentoRepository.save(Pagamento.builder()
                    .reserva(null)
                    .valor(total)
                    .formaPagamento(r.forma())
                    .status(StatusPagamento.PAGO)
                    .ehSinal(false)
                    .origem("DAYUSE")
                    .descricao("Day Use #" + venda.getId() + " - " + pessoas + " pessoa(s) - " + r.data()
                            + (venda.getNomeCliente() != null ? " - " + venda.getNomeCliente() : ""))
                    .operador(venda.getOperador())
                    .build());
            venda.setPagamentoId(pg.getId());
            venda = vendaRepository.save(venda);
        }
        auditoria.detalhe("Day Use #" + venda.getId() + ": " + pessoas + " ingresso(s) para " + r.data() + ", total " + total);
        return paraResponse(venda);
    }

    @Transactional
    public VendaResponse registrarEntrada(Long id, int quantidade) {
        DayUseVenda v = buscar(id);
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
        if (v.getStatus() != StatusDayUse.ATIVA) throw new IllegalArgumentException("Esta venda já está cancelada.");
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
                v.getFormaPagamento(), v.getValorTotal(), v.getPessoas(), v.getEntraram(), v.getStatus(), v.getOperador(),
                v.getCriadoEm(), v.getCanceladaEm(), v.getMotivoCancelamento(),
                v.getItens().stream().map(i -> new ItemVendaResponse(i.getTipoId(), i.getTipoNome(), i.getPrecoUnitario(),
                        i.getQuantidade(), i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))).toList());
    }
}
