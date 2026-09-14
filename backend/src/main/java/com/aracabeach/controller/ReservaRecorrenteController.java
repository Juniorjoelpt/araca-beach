package com.aracabeach.controller;

import com.aracabeach.dto.ReservaRecorrenteRequest;
import com.aracabeach.dto.ReservaRecorrenteResponse;
import com.aracabeach.service.ReservaRecorrenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas-recorrentes")
@RequiredArgsConstructor
public class ReservaRecorrenteController {

    private final ReservaRecorrenteService reservaRecorrenteService;

    @GetMapping
    public List<ReservaRecorrenteResponse> listar() {
        return reservaRecorrenteService.listarAtivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaRecorrenteResponse criar(@Valid @RequestBody ReservaRecorrenteRequest request) {
        return reservaRecorrenteService.criar(request);
    }

    @PatchMapping("/{id}/cancelar")
    public void cancelar(@PathVariable Long id) {
        reservaRecorrenteService.cancelar(id);
    }
}
