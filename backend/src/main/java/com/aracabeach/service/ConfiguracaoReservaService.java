package com.aracabeach.service;

import com.aracabeach.domain.regra.ConfiguracaoReserva;
import com.aracabeach.dto.ConfiguracaoReservaRequest;
import com.aracabeach.repository.ConfiguracaoReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Acesso a configuracao unica das regras de reserva (criada com os padroes se nao existir). */
@Service
@RequiredArgsConstructor
public class ConfiguracaoReservaService {

    private final ConfiguracaoReservaRepository repository;
    private final AuditoriaService auditoria;

    /** Le a configuracao; se ainda nao foi salva, devolve os padroes (sem gravar - seguro em transacao readOnly). */
    @Transactional(readOnly = true)
    public ConfiguracaoReserva obter() {
        return repository.findById(ConfiguracaoReserva.ID_UNICO)
                .orElseGet(() -> ConfiguracaoReserva.builder().id(ConfiguracaoReserva.ID_UNICO).build());
    }

    @Transactional
    public ConfiguracaoReserva atualizar(ConfiguracaoReservaRequest request) {
        ConfiguracaoReserva c = obter();
        String antes = resumo(c);
        c.setHorasCancelamentoGratis(request.horasCancelamentoGratis());
        c.setPercentualMulta(request.percentualMulta());
        c.setPercentualMultaNoShow(request.percentualMultaNoShow());
        c.setDescontoMensalistaPercentual(request.descontoMensalistaPercentual());
        c.setListaEsperaAtiva(request.listaEsperaAtiva());
        c.setHorasAvisoFaltaAula(request.horasAvisoFaltaAula());
        ConfiguracaoReserva salva = repository.save(c);
        auditoria.detalhe("Antes: " + antes + " | Depois: " + resumo(salva));
        return salva;
    }

    private String resumo(ConfiguracaoReserva c) {
        return "cancelamento grátis " + c.getHorasCancelamentoGratis() + "h, multa " + c.getPercentualMulta()
                + "%, no-show " + c.getPercentualMultaNoShow() + "%, desc. mensalista "
                + c.getDescontoMensalistaPercentual() + "%, espera " + c.isListaEsperaAtiva();
    }
}
