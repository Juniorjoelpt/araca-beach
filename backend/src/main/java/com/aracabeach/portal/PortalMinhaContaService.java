package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.domain.reserva.StatusReserva;
import com.aracabeach.repository.MatriculaRepository;
import com.aracabeach.repository.PacoteClienteRepository;
import com.aracabeach.repository.PagamentoMatriculaClienteRepository;
import com.aracabeach.repository.PagamentoMatriculaRepository;
import com.aracabeach.repository.PagamentoMensalidadeRepository;
import com.aracabeach.repository.ReservaRepository;
import com.aracabeach.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Visao "minha conta" do portal: cobrancas em aberto e turmas do cliente logado. */
@Service
@RequiredArgsConstructor
public class PortalMinhaContaService {

    private final PagamentoMensalidadeRepository pagamentoMensalidadeRepository;
    private final PagamentoMatriculaClienteRepository pagamentoMatriculaClienteRepository;
    private final PagamentoMatriculaRepository pagamentoMatriculaRepository;
    private final PacoteClienteRepository pacoteClienteRepository;
    private final ReservaRepository reservaRepository;
    private final MatriculaRepository matriculaRepository;
    private final PagamentoService pagamentoService;

    @Transactional(readOnly = true)
    public List<PortalCobrancaResponse> cobrancas(Cliente cliente) {
        LocalDate hoje = LocalDate.now();
        List<PortalCobrancaResponse> lista = new ArrayList<>();

        pagamentoMensalidadeRepository.pendentesDoCliente(cliente.getId()).forEach(p ->
                lista.add(new PortalCobrancaResponse("MENSALIDADE", "Mensalidade " + p.getReferenciaMes(),
                        p.getValor(), p.getVencimento(), p.getVencimento().isBefore(hoje))));

        pagamentoMatriculaClienteRepository.pendentesDoCliente(cliente.getId()).forEach(p ->
                lista.add(new PortalCobrancaResponse("MATRICULA", "Matrícula " + p.getReferenciaMes(),
                        p.getValor(), p.getVencimento(), p.getVencimento().isBefore(hoje))));

        pagamentoMatriculaRepository.pendentesDoCliente(cliente.getId()).forEach(p ->
                lista.add(new PortalCobrancaResponse("TURMA",
                        "Turma " + p.getMatricula().getTurma().getNome() + " - " + p.getReferenciaMes(),
                        p.getValor(), p.getVencimento(), p.getVencimento().isBefore(hoje))));

        pacoteClienteRepository.findByClienteIdOrderByDataCompraDescIdDesc(cliente.getId()).stream()
                .filter(p -> !p.isPago() && !p.isCancelado())
                .forEach(p -> lista.add(new PortalCobrancaResponse("PACOTE", "Pacote " + p.getPlano().getNome(),
                        p.getValor(), p.getDataCompra(), p.getDataCompra().isBefore(hoje))));

        // Multas de cancelamento/no-show e reservas confirmadas ainda nao pagas.
        for (Reserva r : reservaRepository.findByClienteIdOrderByInicioDesc(cliente.getId())) {
            if (r.getValorTotal() == null || r.getValorTotal().signum() <= 0) {
                continue;
            }
            boolean multa = r.getTaxaCancelamento() != null && r.getTaxaCancelamento().signum() > 0;
            boolean confirmadaPassada = r.getStatus() == StatusReserva.CONFIRMADA || r.getStatus() == StatusReserva.CONCLUIDA;
            if (!multa && !confirmadaPassada) {
                continue;
            }
            BigDecimal pago = pagamentoService.listarPorReserva(r.getId()).stream()
                    .map(p -> p.getValor()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal falta = r.getValorTotal().subtract(pago);
            if (falta.signum() > 0) {
                LocalDate data = r.getInicio().toLocalDate();
                lista.add(new PortalCobrancaResponse(multa ? "MULTA" : "RESERVA",
                        (multa ? "Multa de cancelamento - " : "Reserva - ") + r.getQuadra().getNome()
                                + " em " + data,
                        falta, data, !multa && data.isBefore(hoje)));
            }
        }

        lista.sort(Comparator.comparing(PortalCobrancaResponse::vencimento));
        return lista;
    }

    @Transactional(readOnly = true)
    public List<PortalTurmaResponse> turmas(Cliente cliente) {
        return matriculaRepository.findByClienteIdAndAtivaTrue(cliente.getId()).stream()
                .map(m -> new PortalTurmaResponse(
                        m.getId(),
                        m.getTurma().getNome(),
                        m.getTurma().getTipo(),
                        m.getTurma().getProfessor().getNome(),
                        m.getTurma().getQuadra().getNome(),
                        m.getTurma().getDiaSemana().name(),
                        m.getTurma().getHoraInicio(),
                        m.getTurma().getHoraFim()))
                .toList();
    }
}
