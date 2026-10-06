package com.aracabeach.service;

import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.mensalidade.PagamentoMensalidade;
import com.aracabeach.domain.matricula.PagamentoMatricula;
import com.aracabeach.domain.matriculacliente.PagamentoMatriculaCliente;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.dto.CobrancaPendenteResponse;
import com.aracabeach.dto.PagamentoRequest;
import com.aracabeach.dto.ReservaFinanceiroResponse;
import com.aracabeach.dto.ResumoCaixaResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.PagamentoMatriculaClienteRepository;
import com.aracabeach.repository.PagamentoMatriculaRepository;
import com.aracabeach.repository.PagamentoMensalidadeRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ReservaRepository reservaRepository;
    private final PagamentoMensalidadeRepository pagamentoMensalidadeRepository;
    private final PagamentoMatriculaClienteRepository pagamentoMatriculaClienteRepository;
    private final PagamentoMatriculaRepository pagamentoMatriculaTurmaRepository;
    private final com.aracabeach.repository.PacoteClienteRepository pacoteClienteRepository;

    @Transactional
    public Pagamento registrar(PagamentoRequest request) {
        Reserva reserva = reservaRepository.findById(request.reservaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada: " + request.reservaId()));

        Pagamento pagamento = Pagamento.builder()
                .reserva(reserva)
                .valor(request.valor())
                .formaPagamento(request.formaPagamento())
                .status(StatusPagamento.PAGO)
                .ehSinal(request.ehSinal())
                .build();

        return pagamentoRepository.save(pagamento);
    }

    @Transactional(readOnly = true)
    public List<Pagamento> listarPorReserva(Long reservaId) {
        return pagamentoRepository.findByReservaId(reservaId);
    }

    /**
     * Lista as reservas de um dia (todas as quadras) com o total ja pago
     * e o status de pagamento calculado (PENDENTE / PARCIAL / PAGO).
     */
    @Transactional(readOnly = true)
    public List<ReservaFinanceiroResponse> visaoFinanceiraDoDia(LocalDate data) {
        LocalDateTime inicioDia = LocalDateTime.of(data, LocalTime.MIN);
        LocalDateTime fimDia = LocalDateTime.of(data, LocalTime.MAX);

        List<Reserva> reservas = reservaRepository.findByInicioBetween(inicioDia, fimDia);

        return reservas.stream()
                .map(this::paraFinanceiroResponse)
                .toList();
    }

    private ReservaFinanceiroResponse paraFinanceiroResponse(Reserva reserva) {
        BigDecimal valorPago = pagamentoRepository.findByReservaId(reserva.getId()).stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorTotal = reserva.getValorTotal() != null ? reserva.getValorTotal() : BigDecimal.ZERO;

        String statusPagamento;
        if (valorPago.compareTo(BigDecimal.ZERO) <= 0) {
            // Reserva cancelada sem pagamento nao e uma cobranca pendente de
            // verdade - evita que ela apareca como "Pendente" no Financeiro.
            boolean cancelada = "CANCELADA".equals(reserva.getStatus().name());
            boolean temMulta = reserva.getTaxaCancelamento() != null && reserva.getTaxaCancelamento().signum() > 0;
            statusPagamento = cancelada && !temMulta ? "CANCELADA" : "PENDENTE";
        } else if (valorPago.compareTo(valorTotal) >= 0) {
            statusPagamento = "PAGO";
        } else {
            statusPagamento = "PARCIAL";
        }

        return new ReservaFinanceiroResponse(
                reserva.getId(),
                reserva.getQuadra().getNome(),
                reserva.getCliente().getNome(),
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getStatus(),
                valorTotal,
                valorPago,
                statusPagamento
        );
    }

    /**
     * Resumo do caixa de um dia: total recebido e a quebra por forma de pagamento.
     * Baseado na data em que o pagamento foi registrado (nao na data da reserva).
     */
    @Transactional(readOnly = true)
    public ResumoCaixaResponse resumoCaixa(LocalDate data) {
        LocalDateTime inicioDia = LocalDateTime.of(data, LocalTime.MIN);
        LocalDateTime fimDia = LocalDateTime.of(data, LocalTime.MAX);

        List<Pagamento> pagamentos = pagamentoRepository.findByCriadoEmBetween(inicioDia, fimDia);

        BigDecimal total = pagamentos.stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> porForma = pagamentos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getFormaPagamento().name(),
                        Collectors.reducing(BigDecimal.ZERO, Pagamento::getValor, BigDecimal::add)
                ));

        return new ResumoCaixaResponse(data, total, porForma, pagamentos.size());
    }

    /**
     * Lista, de forma unificada, todas as cobrancas pendentes (nao pagas) das
     * tres formas de cobranca recorrente do sistema: Mensalidade (por
     * ReservaRecorrente), MatriculaCliente (varios horarios combinados) e
     * Matricula de Turma (aula em grupo). Essas cobrancas nao aparecem na
     * "visao do dia" por reserva porque nao estao necessariamente ligadas a
     * uma reserva especifica (ver visaoFinanceiraDoDia).
     */
    @Transactional(readOnly = true)
    public List<CobrancaPendenteResponse> listarCobrancasPendentes() {
        LocalDate hoje = LocalDate.now();
        List<CobrancaPendenteResponse> cobrancas = new ArrayList<>();

        for (PagamentoMensalidade p : pagamentoMensalidadeRepository.findByPagoFalseOrderByVencimentoAsc()) {
            cobrancas.add(new CobrancaPendenteResponse(
                    p.getId(),
                    "MENSALIDADE",
                    p.getMensalidade().getReservaRecorrente().getCliente().getNome(),
                    "Mensalidade - " + p.getReferenciaMes(),
                    p.getReferenciaMes(),
                    p.getValor(),
                    p.getVencimento(),
                    p.getVencimento().isBefore(hoje)
            ));
        }

        for (PagamentoMatriculaCliente p : pagamentoMatriculaClienteRepository.findByPagoFalseOrderByVencimentoAsc()) {
            cobrancas.add(new CobrancaPendenteResponse(
                    p.getId(),
                    "MATRICULA_CLIENTE",
                    p.getMatriculaCliente().getCliente().getNome(),
                    "Matricula - " + p.getReferenciaMes(),
                    p.getReferenciaMes(),
                    p.getValor(),
                    p.getVencimento(),
                    p.getVencimento().isBefore(hoje)
            ));
        }

        for (PagamentoMatricula p : pagamentoMatriculaTurmaRepository.findByPagoFalseOrderByVencimentoAsc()) {
            cobrancas.add(new CobrancaPendenteResponse(
                    p.getId(),
                    "MATRICULA_TURMA",
                    p.getMatricula().getCliente().getNome(),
                    "Matricula de turma - " + p.getReferenciaMes(),
                    p.getReferenciaMes(),
                    p.getValor(),
                    p.getVencimento(),
                    p.getVencimento().isBefore(hoje)
            ));
        }

        for (com.aracabeach.domain.pacote.PacoteCliente p : pacoteClienteRepository.findByPagoFalseAndCanceladoFalseOrderByDataCompraAsc()) {
            cobrancas.add(new CobrancaPendenteResponse(
                    p.getId(),
                    "PACOTE",
                    p.getCliente().getNome(),
                    "Pacote - " + p.getPlano().getNome(),
                    p.getDataCompra().toString().substring(0, 7),
                    p.getValor(),
                    p.getDataCompra(),
                    p.getDataCompra().isBefore(hoje)
            ));
        }

        cobrancas.sort(Comparator.comparing(CobrancaPendenteResponse::vencimento));
        return cobrancas;
    }

    /**
     * Exclui um pagamento registrado por engano (por exemplo, um pagamento
     * registrado diretamente contra uma reserva gerada por matricula/
     * mensalidade, que deveria ter sido registrado como cobranca ao inves
     * disso). Nao afeta PagamentoMensalidade/PagamentoMatricula/
     * PagamentoMatriculaCliente - essas tem seus proprios fluxos de
     * pagamento e nao sao excluidas por aqui.
     */
    @Transactional
    public void excluir(Long id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento nao encontrado: " + id));
        pagamentoRepository.delete(pagamento);
    }
}
