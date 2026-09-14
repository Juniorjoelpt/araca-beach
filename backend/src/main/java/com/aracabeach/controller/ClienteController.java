package com.aracabeach.controller;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.dto.ClienteHistoricoResponse;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.service.ClienteHistoricoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final ClienteHistoricoService clienteHistoricoService;

    @GetMapping
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @PostMapping
    public Cliente criar(@Valid @RequestBody Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @PutMapping("/{id}")
    public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        cliente.setId(id);
        return clienteRepository.save(cliente);
    }

    @GetMapping("/{id}/historico")
    public ClienteHistoricoResponse historico(@PathVariable Long id) {
        return clienteHistoricoService.obter(id);
    }
}
