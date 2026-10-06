package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.domain.regra.ConfiguracaoReserva;
import com.aracabeach.dto.AulaPacoteResponse;
import com.aracabeach.dto.ListaEsperaRequest;
import com.aracabeach.dto.ListaEsperaResponse;
import com.aracabeach.dto.PacoteClienteResponse;
import com.aracabeach.service.ConfiguracaoReservaService;
import com.aracabeach.service.ListaEsperaService;
import com.aracabeach.service.PacoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints do portal para cobrancas, pacotes/aulas, turmas e lista de espera (exigem login de cliente). */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalMinhaContaController {

    private final PortalMinhaContaService minhaContaService;
    private final PacoteService pacoteService;
    private final ListaEsperaService listaEsperaService;
    private final ConfiguracaoReservaService configuracaoService;

    @GetMapping("/cobrancas")
    public List<PortalCobrancaResponse> cobrancas(@AuthenticationPrincipal Cliente cliente) {
        return minhaContaService.cobrancas(cliente);
    }

    @GetMapping("/turmas")
    public List<PortalTurmaResponse> turmas(@AuthenticationPrincipal Cliente cliente) {
        return minhaContaService.turmas(cliente);
    }

    @GetMapping("/pacotes")
    public List<PacoteClienteResponse> pacotes(@AuthenticationPrincipal Cliente cliente) {
        return pacoteService.pacotesDoCliente(cliente.getId());
    }

    @GetMapping("/aulas")
    public List<AulaPacoteResponse> aulas(@AuthenticationPrincipal Cliente cliente) {
        return pacoteService.aulasDoCliente(cliente.getId());
    }

    @PatchMapping("/aulas/{id}/avisar-falta")
    public AulaPacoteResponse avisarFalta(@AuthenticationPrincipal Cliente cliente, @PathVariable Long id) {
        return pacoteService.avisarFalta(id, cliente.getId());
    }

    @GetMapping("/lista-espera")
    public List<ListaEsperaResponse> listaEspera(@AuthenticationPrincipal Cliente cliente) {
        return listaEsperaService.listarDoCliente(cliente.getId());
    }

    @PostMapping("/lista-espera")
    @ResponseStatus(HttpStatus.CREATED)
    public ListaEsperaResponse entrarNaListaEspera(@AuthenticationPrincipal Cliente cliente,
                                                   @Valid @RequestBody ListaEsperaRequest request) {
        return listaEsperaService.entrar(cliente.getId(), request);
    }

    @DeleteMapping("/lista-espera/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sairDaListaEspera(@AuthenticationPrincipal Cliente cliente, @PathVariable Long id) {
        listaEsperaService.cancelar(id, cliente.getId());
    }

    /** Politica de cancelamento vigente, para mostrar antes de reservar. */
    @GetMapping("/politica")
    public ConfiguracaoReserva politica() {
        return configuracaoService.obter();
    }
}
