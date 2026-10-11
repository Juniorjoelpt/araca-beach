package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.dto.DayUseDtos.*;
import com.aracabeach.service.DayUseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Day Use no portal do cliente: ver vagas e valores, reservar e acompanhar. O pagamento e confirmado pela recepcao. */
@RestController
@RequestMapping("/api/portal/dayuse")
@RequiredArgsConstructor
public class PortalDayUseController {

    private final DayUseService service;

    @GetMapping("/disponibilidade")
    public DisponibilidadeResponse disponibilidade(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.disponibilidade(data);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse reservar(@AuthenticationPrincipal Cliente cliente, @Valid @RequestBody PortalDayUseRequest request) {
        return service.reservarPeloPortal(cliente, request);
    }

    @GetMapping("/reservas")
    public List<VendaResponse> minhas(@AuthenticationPrincipal Cliente cliente) {
        return service.minhasReservas(cliente);
    }

    @PatchMapping("/reservas/{id}/cancelar")
    public VendaResponse cancelar(@AuthenticationPrincipal Cliente cliente, @PathVariable Long id) {
        return service.cancelarPeloPortal(cliente, id);
    }
}
