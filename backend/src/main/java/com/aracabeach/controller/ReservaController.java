package com.aracabeach.controller;

import com.aracabeach.domain.reserva.Reserva;
import com.aracabeach.dto.PoliticaCancelamentoResponse;
import com.aracabeach.dto.ReservaRequest;
import com.aracabeach.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public List<Reserva> listar() {
        return reservaService.listarTodas();
    }

    @GetMapping("/agenda")
    public List<Reserva> agenda(
            @RequestParam Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return reservaService.listarPorQuadraEPeriodo(quadraId, inicio, fim);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reserva criar(@Valid @RequestBody ReservaRequest request) {
        return reservaService.criar(request);
    }

    @PatchMapping("/{id}/cancelar")
    public Reserva cancelar(@PathVariable Long id,
                            @RequestParam(defaultValue = "false") boolean isentarMulta) {
        return reservaService.cancelar(id, isentarMulta);
    }

    @PatchMapping("/{id}/nao-compareceu")
    public Reserva naoCompareceu(@PathVariable Long id,
                                 @RequestParam(defaultValue = "false") boolean isentarMulta) {
        return reservaService.marcarNaoCompareceu(id, isentarMulta);
    }

    /** O que acontece se cancelar agora (multa ou nao). */
    @GetMapping("/{id}/politica-cancelamento")
    public PoliticaCancelamentoResponse politicaCancelamento(@PathVariable Long id) {
        return reservaService.simularCancelamento(id);
    }
}
