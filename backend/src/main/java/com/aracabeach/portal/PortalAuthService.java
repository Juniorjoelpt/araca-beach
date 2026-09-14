package com.aracabeach.portal;

import com.aracabeach.config.JwtService;
import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Cadastro do cliente no portal. Se ja existir um registro de Cliente
     * com esse e-mail (por exemplo, cadastrado antes pela recepcao) e ele
     * ainda nao tiver senha, "reivindicamos" esse registro em vez de criar
     * um duplicado - assim o cliente ja chega com o historico de reservas
     * anteriores vinculado a conta nova.
     */
    @Transactional
    public PortalAuthResponse registrar(PortalRegistroRequest request) {
        Cliente cliente = clienteRepository.findByEmail(request.email()).orElse(null);

        if (cliente != null && cliente.isPossuiAcessoPortal()) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail. Faça login.");
        }

        if (cliente == null) {
            cliente = Cliente.builder()
                    .nome(request.nome())
                    .email(request.email())
                    .telefone(request.telefone())
                    .build();
        } else {
            cliente.setNome(request.nome());
            if (request.telefone() != null && !request.telefone().isBlank()) {
                cliente.setTelefone(request.telefone());
            }
        }

        cliente.setSenhaHash(passwordEncoder.encode(request.senha()));
        cliente = clienteRepository.save(cliente);

        String token = jwtService.gerarToken(cliente, JwtService.TIPO_CLIENTE);
        return new PortalAuthResponse(token, cliente.getId(), cliente.getNome(), cliente.getEmail());
    }

    @Transactional(readOnly = true)
    public PortalAuthResponse login(PortalLoginRequest request) {
        Cliente cliente = clienteRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

        if (!cliente.isPossuiAcessoPortal() || !passwordEncoder.matches(request.senha(), cliente.getSenhaHash())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }

        String token = jwtService.gerarToken(cliente, JwtService.TIPO_CLIENTE);
        return new PortalAuthResponse(token, cliente.getId(), cliente.getNome(), cliente.getEmail());
    }
}
