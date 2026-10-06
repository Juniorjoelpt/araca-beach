package com.aracabeach.service;

import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.domain.regra.ConfiguracaoReserva;
import com.aracabeach.domain.regra.RegraPreco;
import com.aracabeach.dto.PrecoResponse;
import com.aracabeach.repository.MensalidadeRepository;
import com.aracabeach.repository.RegraPrecoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula o valor de uma reserva em blocos de 15 minutos: cada bloco usa a
 * regra de preco (pico/fora de pico) que vale para o horario em que ele
 * comeca, ou o valorHora da quadra quando nenhuma regra se aplica. Depois
 * aplica o desconto de mensalista. Substitui o calculo antigo
 * (valorHora x horas) que tambem falhava em duracoes nao exatas.
 */
@Service
@RequiredArgsConstructor
public class PrecificacaoService {

    private static final int BLOCO_MINUTOS = 15;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final RegraPrecoRepository regraPrecoRepository;
    private final MensalidadeRepository mensalidadeRepository;
    private final ConfiguracaoReservaService configuracaoService;

    @Transactional(readOnly = true)
    public boolean ehMensalista(Long clienteId) {
        return clienteId != null && mensalidadeRepository.existsByAtivaTrueAndReservaRecorrenteClienteId(clienteId);
    }

    @Transactional(readOnly = true)
    public PrecoResponse calcular(Quadra quadra, LocalDateTime inicio, LocalDateTime fim, Long clienteId) {
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O horario de termino deve ser posterior ao de inicio.");
        }
        List<RegraPreco> regras = regraPrecoRepository.findByAtivaTrue();

        BigDecimal bruto = BigDecimal.ZERO;
        LocalDateTime cursor = inicio;
        long minutos = Duration.between(inicio, fim).toMinutes();
        while (cursor.isBefore(fim)) {
            long restante = Duration.between(cursor, fim).toMinutes();
            long duracao = Math.min(BLOCO_MINUTOS, restante);
            BigDecimal valorHora = valorHoraNo(quadra, regras, cursor);
            bruto = bruto.add(valorHora.multiply(BigDecimal.valueOf(duracao))
                    .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP));
            cursor = cursor.plusMinutes(BLOCO_MINUTOS);
        }
        bruto = bruto.setScale(2, RoundingMode.HALF_UP);

        boolean mensalista = ehMensalista(clienteId);
        BigDecimal desconto = BigDecimal.ZERO;
        if (mensalista) {
            ConfiguracaoReserva cfg = configuracaoService.obter();
            desconto = bruto.multiply(cfg.getDescontoMensalistaPercentual())
                    .divide(CEM, 2, RoundingMode.HALF_UP);
        }
        return new PrecoResponse(bruto, desconto, bruto.subtract(desconto), mensalista, (int) minutos);
    }

    /** Valor/hora vigente para um instante (usado tambem para exibir o preco de cada slot). */
    @Transactional(readOnly = true)
    public BigDecimal valorHoraNo(Quadra quadra, LocalDateTime instante) {
        return valorHoraNo(quadra, regraPrecoRepository.findByAtivaTrue(), instante);
    }

    private BigDecimal valorHoraNo(Quadra quadra, List<RegraPreco> regras, LocalDateTime instante) {
        DayOfWeek dia = instante.getDayOfWeek();
        LocalTime hora = instante.toLocalTime();
        Optional<RegraPreco> vencedora = regras.stream()
                .filter(r -> r.getQuadra() == null || r.getQuadra().getId().equals(quadra.getId()))
                .filter(r -> aplicaNoDia(r, dia))
                .filter(r -> aplicaNaHora(r, hora))
                .max(Comparator.<RegraPreco, Integer>comparing(r -> r.getQuadra() != null ? 1 : 0)
                        .thenComparing(RegraPreco::getId));
        return vencedora.map(RegraPreco::getValorHora).orElse(quadra.getValorHora());
    }

    private boolean aplicaNoDia(RegraPreco r, DayOfWeek dia) {
        if (r.getDiasSemana() == null || r.getDiasSemana().isBlank()) {
            return true;
        }
        Set<String> dias = Arrays.stream(r.getDiasSemana().split(","))
                .map(String::trim).collect(Collectors.toSet());
        return dias.contains(dia.name());
    }

    private boolean aplicaNaHora(RegraPreco r, LocalTime hora) {
        // Janela que cruza a meia-noite (ex.: 22:00-02:00)
        if (r.getHoraFim().isAfter(r.getHoraInicio())) {
            return !hora.isBefore(r.getHoraInicio()) && hora.isBefore(r.getHoraFim());
        }
        return !hora.isBefore(r.getHoraInicio()) || hora.isBefore(r.getHoraFim());
    }
}
