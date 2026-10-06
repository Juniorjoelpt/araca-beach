package com.aracabeach.controller;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.dto.ClienteHistoricoResponse;
import com.aracabeach.service.ClienteHistoricoService;
import com.aracabeach.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteHistoricoService clienteHistoricoService;

    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listar();
    }

    @GetMapping("/{id}")
    public Cliente buscar(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    public Cliente criar(@Valid @RequestBody Cliente cliente) {
        return clienteService.criar(cliente);
    }

    @PutMapping("/{id}")
    public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        return clienteService.atualizar(id, cliente);
    }

    @PatchMapping("/{id}/confirmar-email")
    public Cliente confirmarEmail(@PathVariable Long id) {
        return clienteService.confirmarEmailManualmente(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historico")
    public ClienteHistoricoResponse historico(@PathVariable Long id) {
        return clienteHistoricoService.obter(id);
    }
}
