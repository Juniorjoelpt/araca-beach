package com.aracabeach.controller;

import com.aracabeach.dto.DiaLiberadoPreviaResponse;
import com.aracabeach.dto.DiaLiberadoRequest;
import com.aracabeach.dto.DiaLiberadoResponse;
import com.aracabeach.service.DiaLiberadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dias-liberados")
@RequiredArgsConstructor
public class DiaLiberadoController {

    private final DiaLiberadoService service;

    @GetMapping
    public List<DiaLiberadoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/previa")
    public DiaLiberadoPreviaResponse previa(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.previa(data);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiaLiberadoResponse liberar(@Valid @RequestBody DiaLiberadoRequest request) {
        return service.liberar(request.data(), request.motivo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desfazer(@PathVariable Long id) {
        service.desfazer(id);
    }
}
