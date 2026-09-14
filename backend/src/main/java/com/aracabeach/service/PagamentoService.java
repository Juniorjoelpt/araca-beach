package com.aracabeach.service;

import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.dto.PagamentoRequest;
import com.aracabeach.dto.ReservaFinanceiroResponse;
import com.aracabeach.dto.ResumoCaixaResponse;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ReservaRepository reservaRepository;

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
            statusPagamento = "PENDENTE";
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
}
