package com.aracabeach.controller;

import com.aracabeach.domain.professor.Professor;
import com.aracabeach.dto.ProfessorRequest;
import com.aracabeach.repository.ProfessorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professores")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorRepository professorRepository;

    @GetMapping
    public List<Professor> listar() {
        return professorRepository.findAll();
    }

    @PostMapping
    public Professor criar(@Valid @RequestBody ProfessorRequest request) {
        Professor professor = Professor.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .especialidade(request.especialidade())
                .percentualComissao(request.percentualComissao())
                .ativo(true)
                .build();
        return professorRepository.save(professor);
    }
}
