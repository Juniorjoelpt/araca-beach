package com.aracabeach.controller;

import com.aracabeach.dto.ListaEsperaRequest;
import com.aracabeach.dto.ListaEsperaResponse;
import com.aracabeach.service.ListaEsperaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lista de espera pelo painel da equipe (o cliente usa /api/portal/lista-espera). */
@RestController
@RequestMapping("/api/lista-espera")
@RequiredArgsConstructor
public class ListaEsperaController {

    private final ListaEsperaService listaEsperaService;

    @GetMapping
    public List<ListaEsperaResponse> listar() {
        return listaEsperaService.listarAbertas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListaEsperaResponse adicionar(@Valid @RequestBody ListaEsperaRequest request) {
        if (request.clienteId() == null) {
            throw new IllegalArgumentException("Informe o cliente.");
        }
        return listaEsperaService.entrar(request.clienteId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id) {
        listaEsperaService.cancelar(id, null);
    }
}
