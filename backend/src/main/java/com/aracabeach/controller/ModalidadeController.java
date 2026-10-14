package com.aracabeach.controller;

import com.aracabeach.domain.quadra.Modalidade;
import com.aracabeach.dto.ModalidadeRequest;
import com.aracabeach.service.ModalidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modalidades")
@RequiredArgsConstructor
public class ModalidadeController {

    private final ModalidadeService modalidadeService;

    @GetMapping
    public List<Modalidade> listar() {
        return modalidadeService.listar();
    }

    @PostMapping
    public Modalidade criar(@Valid @RequestBody ModalidadeRequest request) {
        return modalidadeService.criar(request.nome());
    }

    @PutMapping("/{id}")
    public Modalidade renomear(@PathVariable Long id, @Valid @RequestBody ModalidadeRequest request) {
        return modalidadeService.renomear(id, request.nome());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        modalidadeService.excluir(id);
    }
}
