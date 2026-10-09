package com.aracabeach.service;

import com.aracabeach.domain.financeiro.Pagamento;
import com.aracabeach.domain.financeiro.StatusPagamento;
import com.aracabeach.domain.mensalidade.Mensalidade;
import com.aracabeach.domain.mensalidade.PagamentoMensalidade;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.ReservaRecorrente;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.dto.MensalidadeRequest;
import com.aracabeach.dto.MensalidadeResponse;
import com.aracabeach.dto.PagamentoMensalidadeResponse;
import com.aracabeach.dto.RegistrarPagamentoMensalidadeRequest;
import com.aracabeach.exception.RecursoNaoEncontradoException;
import com.aracabeach.repository.MensalidadeRepository;
import com.aracabeach.repository.PagamentoMensalidadeRepository;
import com.aracabeach.repository.PagamentoRepository;
import com.aracabeach.repository.ReservaRecorrenteRepository;
import com.aracabeach.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia os planos de mensalidade de clientes com reserva recorrente:
 * em vez de pagar cada sessao avulsamente, o cliente paga um valor fixo
 * por mes. Ao ativar a mensalidade, as reservas futuras ja geradas pela
 * recorrencia tem o valorTotal zerado (a cobranca passa a ser mensal, nao
 * por sessao). A geracao da cobranca do mes segue o mesmo padrao de
 * DespesaRecorrenteService (idempotente via ultimoMesGerado).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MensalidadeService {

    private final MensalidadeRepository mensalidadeRepository;
    private final PagamentoMensalidadeRepository pagamentoMensalidadeRepository;
    private final ReservaRecorrenteRepository reservaRecorrenteRepository;
    private final ReservaRepository reservaRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public MensalidadeResponse criar(MensalidadeRequest request) {
        ReservaRecorrente recorrencia = reservaRecorrenteRepository.findById(request.reservaRecorrenteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Reserva recorrente não encontrada: " + request.reservaRecorrenteId()));

        if (!recorrencia.isAtiva()) {
            throw new IllegalArgumentException("Não é possível criar mensalidade para uma recorrência inativa.");
        }

        mensalidadeRepository.findByReservaRecorrenteId(recorrencia.getId()).ifPresent(m -> {
            throw new IllegalArgumentException("Essa reserva recorrente já possui uma mensalidade cadastrada.");
        });

        Mensalidade mensalidade = Mensalidade.builder()
                .reservaRecorrente(recorrencia)
                .valorMensal(request.valorMensal())
                .diaVencimento(request.diaVencimento())
                .ativa(true)
                .build();
        mensalidade = mensalidadeRepository.save(mensalidade);

        // A mensalidade substitui a cobranca por sessao: zera o valor das
        // reservas futuras ja geradas por essa recorrencia para a recepcao
        // nao cobrar duas vezes pelo mesmo periodo.
        LocalDateTime agora = LocalDateTime.now();
        List<Reserva> futuras = reservaRepository.findByReservaRecorrenteId(recorrencia.getId()).stream()
                .filter(r -> r.getInicio().isAfter(agora) && r.getStatus() != StatusReserva.CANCELADA)
                .toList();
        futuras.forEach(r -> r.setValorTotal(BigDecimal.ZERO));
        reservaRepository.saveAll(futuras);

        // Gera a cobranca do mes atual na hora, em vez de esperar o scheduler
        // da 1h15 - sem isso, uma mensalidade criada de tarde so teria
        // cobranca pendente no dia seguinte, dando a impressao de que a
        // primeira mensalidade "sumiu" (mesmo ajuste ja feito em
        // MatriculaClienteService.criar()).
        gerarCobrancaDoMes(mensalidade, YearMonth.now());

        return paraResponse(mensalidade);
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponse> listarAtivas() {
        return mensalidadeRepository.findByAtivaTrue().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional
    public void desativar(Long id) {
        Mensalidade mensalidade = mensalidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensalidade não encontrada: " + id));
        mensalidade.setAtiva(false);
        mensalidadeRepository.save(mensalidade);
    }

    @Transactional(readOnly = true)
    public List<PagamentoMensalidadeResponse> listarPagamentos(Long mensalidadeId) {
        return pagamentoMensalidadeRepository.findByMensalidadeIdOrderByVencimentoDesc(mensalidadeId).stream()
                .map(this::paraPagamentoResponse)
                .toList();
    }

    @Transactional
    public PagamentoMensalidadeResponse registrarPagamento(Long pagamentoId, RegistrarPagamentoMensalidadeRequest request) {
        PagamentoMensalidade pagamento = pagamentoMensalidadeRepository.findById(pagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança de mensalidade não encontrada: " + pagamentoId));

        if (pagamento.isPago()) {
            throw new IllegalArgumentException("Essa cobrança já foi paga.");
        }

        pagamento.setPago(true);
        pagamento.setDataPagamento(LocalDate.now());
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamento = pagamentoMensalidadeRepository.save(pagamento);

        // Registra tambem em Pagamento (sem reserva vinculada) para que o
        // valor entre automaticamente no fechamento de caixa do dia.
        Pagamento pagamentoCaixa = Pagamento.builder()
                .reserva(null)
                .valor(pagamento.getValor())
                .formaPagamento(pagamento.getFormaPagamento())
                .status(StatusPagamento.PAGO)
                .ehSinal(false)
                .origem("MENSALIDADE")
                .descricao("Mensalidade " + pagamento.getReferenciaMes() + " - "
                        + pagamento.getMensalidade().getReservaRecorrente().getCliente().getNome())
                .operador(com.aracabeach.config.OperadorAtual.login())
                .build();
        pagamentoRepository.save(pagamentoCaixa);

        return paraPagamentoResponse(pagamento);
    }

    /**
     * Para cada mensalidade ativa que ainda nao gerou a cobranca do mes
     * atual, cria a PagamentoMensalidade pendente e marca o mes como gerado.
     * Idempotente: rodar mais de uma vez no mesmo mes nao duplica cobrancas.
     */
    @Transactional
    public int gerarCobrancasDoMes() {
        YearMonth mesAtual = YearMonth.now();
        int geradas = 0;

        for (Mensalidade mensalidade : mensalidadeRepository.findByAtivaTrue()) {
            if (gerarCobrancaDoMes(mensalidade, mesAtual)) {
                geradas++;
            }
        }

        if (geradas > 0) {
            log.info("{} cobrança(s) de mensalidade geradas para o mês {}.", geradas, mesAtual);
        }
        return geradas;
    }

    /**
     * Gera (se ainda nao existir) a cobranca de uma mensalidade para o mes
     * informado. Usado tanto pelo job diario (gerarCobrancasDoMes, para
     * todas as mensalidades ativas) quanto na criacao da mensalidade (para
     * nao esperar o proximo ciclo do scheduler so para ter a primeira
     * cobranca). Retorna true se uma cobranca nova foi criada.
     */
    private boolean gerarCobrancaDoMes(Mensalidade mensalidade, YearMonth mes) {
        String mesStr = mes.toString();

        if (mesStr.equals(mensalidade.getUltimoMesGerado())) {
            return false;
        }

        Optional<PagamentoMensalidade> existente =
                pagamentoMensalidadeRepository.findByMensalidadeIdAndReferenciaMes(mensalidade.getId(), mesStr);
        if (existente.isPresent()) {
            mensalidade.setUltimoMesGerado(mesStr);
            mensalidadeRepository.save(mensalidade);
            return false;
        }

        int dia = Math.min(mensalidade.getDiaVencimento(), mes.lengthOfMonth());
        LocalDate vencimento = mes.atDay(dia);

        PagamentoMensalidade cobranca = PagamentoMensalidade.builder()
                .mensalidade(mensalidade)
                .referenciaMes(mesStr)
                .valor(mensalidade.getValorMensal())
                .vencimento(vencimento)
                .pago(false)
                .build();
        pagamentoMensalidadeRepository.save(cobranca);

        mensalidade.setUltimoMesGerado(mesStr);
        mensalidadeRepository.save(mensalidade);
        return true;
    }

    private MensalidadeResponse paraResponse(Mensalidade m) {
        ReservaRecorrente r = m.getReservaRecorrente();
        return new MensalidadeResponse(
                m.getId(),
                r.getId(),
                r.getCliente().getNome(),
                r.getQuadra().getNome(),
                r.getDiaSemana().name(),
                r.getHoraInicio().toString(),
                r.getHoraFim().toString(),
                m.getValorMensal(),
                m.getDiaVencimento(),
                m.isAtiva()
        );
    }

    private PagamentoMensalidadeResponse paraPagamentoResponse(PagamentoMensalidade p) {
        return new PagamentoMensalidadeResponse(
                p.getId(),
                p.getMensalidade().getId(),
                p.getMensalidade().getReservaRecorrente().getCliente().getNome(),
                p.getReferenciaMes(),
                p.getValor(),
                p.getVencimento(),
                p.isPago(),
                p.getDataPagamento(),
                p.getFormaPagamento()
        );
    }
}
