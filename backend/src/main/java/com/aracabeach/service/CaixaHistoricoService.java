package com.aracabeach.service;

import com.aracabeach.config.OperadorAtual;
import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.usuario.Usuario;
import com.aracabeach.dto.CaixaHistoricoResponse;
import com.aracabeach.dto.CaixaHistoricoResponse.Lancamento;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Historico de caixa: extrato dos recebimentos (tabela pagamentos, onde reservas, restaurante, loja,
 * mensalidades, matriculas e pacotes caem). O operador comum so enxerga o que ele mesmo recebeu;
 * o administrador enxerga tudo e pode filtrar por operador.
 */
@Service
@RequiredArgsConstructor
public class CaixaHistoricoService {

    public static final String SEM_OPERADOR = "Sem operador registrado";
    private static final long MAX_DIAS = 366;

    private final PagamentoRepository pagamentoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public CaixaHistoricoResponse consultar(LocalDate inicio, LocalDate fim, String operador, String origem, String forma) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe o periodo (data inicial e final).");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }
        if (inicio.plusDays(MAX_DIAS).isBefore(fim)) {
            throw new IllegalArgumentException("O periodo maximo e de 1 ano.");
        }

        // Operador comum: sempre so o proprio historico, ignorando qualquer filtro enviado.
        String filtroOperador = vazioParaNulo(operador);
        if (!OperadorAtual.admin()) {
            filtroOperador = OperadorAtual.login();
            if (filtroOperador == null) {
                throw new IllegalArgumentException("Usuario nao identificado.");
            }
        }
        final String op = filtroOperador;
        final String org = vazioParaNulo(origem);
        final String frm = vazioParaNulo(forma);

        Map<String, String> nomes = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getLogin, Usuario::getNome, (a, b) -> a));

        List<Pagamento> pagamentos = pagamentoRepository.findByCriadoEmBetween(
                        LocalDateTime.of(inicio, LocalTime.MIN), LocalDateTime.of(fim, LocalTime.MAX)).stream()
                .filter(p -> p.getStatus() != StatusPagamento.ESTORNADO)
                .sorted(Comparator.comparing(Pagamento::getCriadoEm).thenComparing(Pagamento::getId))
                .toList();

        List<Lancamento> lancamentos = pagamentos.stream()
                .map(p -> paraLancamento(p, nomes))
                .filter(l -> op == null || op.equalsIgnoreCase(l.operador() == null ? "" : l.operador()))
                .filter(l -> org == null || org.equals(l.origem()))
                .filter(l -> frm == null || frm.equals(l.forma()))
                .toList();

        BigDecimal total = lancamentos.stream().map(Lancamento::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CaixaHistoricoResponse(inicio, fim, op, total, lancamentos.size(),
                somar(lancamentos, Lancamento::forma),
                somar(lancamentos, Lancamento::origem),
                somar(lancamentos, l -> l.operador() == null ? SEM_OPERADOR : l.operadorNome()),
                lancamentos);
    }

    /** Logins disponiveis para o filtro do administrador. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> operadores() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(u -> Map.of("login", u.getLogin(), "nome", u.getNome()))
                .toList();
    }

    private Lancamento paraLancamento(Pagamento p, Map<String, String> nomes) {
        String origem = p.getOrigem() != null ? p.getOrigem() : (p.getReserva() != null ? "RESERVA" : "OUTROS");
        String descricao = p.getDescricao();
        if (descricao == null) {
            descricao = p.getReserva() != null
                    ? "Reserva #" + p.getReserva().getId() + " - " + p.getReserva().getQuadra().getNome()
                            + " - " + p.getReserva().getCliente().getNome() + (p.isEhSinal() ? " (sinal)" : "")
                    : "Recebimento (sem detalhe)";
        }
        String login = p.getOperador();
        String nome = login == null ? null : nomes.getOrDefault(login, login);
        return new Lancamento(p.getId(), p.getCriadoEm(), origem, descricao,
                p.getFormaPagamento().name(), p.getValor(), login, nome);
    }

    private static Map<String, BigDecimal> somar(List<Lancamento> ls, java.util.function.Function<Lancamento, String> chave) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (Lancamento l : ls) {
            mapa.merge(chave.apply(l), l.valor(), BigDecimal::add);
        }
        return mapa;
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
