package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.dto.PrecoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalReservaController {

    private final PortalReservaService portalReservaService;

    @GetMapping("/quadras")
    public List<PortalQuadraResponse> quadras() {
        return portalReservaService.listarQuadrasDisponiveis();
    }

    @GetMapping("/disponibilidade")
    public List<PortalSlotResponse> disponibilidade(
            @RequestParam Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return portalReservaService.disponibilidade(quadraId, data);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public PortalReservaResponse criarReserva(
            @AuthenticationPrincipal Cliente cliente,
            @Valid @RequestBody PortalReservaRequest request) {
        return portalReservaService.criarReserva(cliente, request);
    }

    @GetMapping("/reservas")
    public List<PortalReservaResponse> minhasReservas(@AuthenticationPrincipal Cliente cliente) {
        return portalReservaService.minhasReservas(cliente);
    }

    @PatchMapping("/reservas/{id}/cancelar")
    public PortalReservaResponse cancelarReserva(@AuthenticationPrincipal Cliente cliente, @PathVariable Long id) {
        return portalReservaService.cancelarReserva(cliente, id);
    }

    @GetMapping("/preco")
    public PrecoResponse preco(
            @AuthenticationPrincipal Cliente cliente,
            @RequestParam Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return portalReservaService.preco(cliente, quadraId, inicio, fim);
    }
}
