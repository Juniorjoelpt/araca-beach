package com.aracabeach.controller;

import com.aracabeach.domain.quadra.Quadra;
import com.aracabeach.repository.QuadraRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quadras")
@RequiredArgsConstructor
public class QuadraController {

    private final QuadraRepository quadraRepository;

    @GetMapping
    public List<Quadra> listar() {
        return quadraRepository.findAll();
    }

    @PostMapping
    public Quadra criar(@Valid @RequestBody Quadra quadra) {
        return quadraRepository.save(quadra);
    }

    @PutMapping("/{id}")
    public Quadra atualizar(@PathVariable Long id, @Valid @RequestBody Quadra quadra) {
        quadra.setId(id);
        return quadraRepository.save(quadra);
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable Long id) {
        quadraRepository.deleteById(id);
    }
}
