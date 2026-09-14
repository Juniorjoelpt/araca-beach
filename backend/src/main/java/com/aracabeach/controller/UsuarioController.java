package com.aracabeach.controller;

import com.aracabeach.dto.TrocarSenhaRequest;
import com.aracabeach.dto.UsuarioRequest;
import com.aracabeach.dto.UsuarioResponse;
import com.aracabeach.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.criar(request);
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return usuarioService.alterarStatus(id, Boolean.TRUE.equals(body.get("ativo")));
    }

    @PatchMapping("/{id}/senha")
    public UsuarioResponse trocarSenha(@PathVariable Long id, @Valid @RequestBody TrocarSenhaRequest request) {
        return usuarioService.trocarSenha(id, request);
    }
}
