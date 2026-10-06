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

        String telefone = request.telefone().trim();
        if (apenasDigitos(telefone).length() < 10) {
            throw new IllegalArgumentException("Informe o telefone com DDD.");
        }

        // O cadastro do portal ja e o cadastro de cliente/aluno da arena (a mesma
        // entidade Cliente alimenta reservas, pacotes, turmas e cobrancas). Para nao
        // duplicar, se o telefone ja existe em OUTRO cadastro, nao criamos um novo
        // nem vinculamos automaticamente (qualquer pessoa poderia assumir o historico
        // de outra digitando o telefone): a recepcao vincula o e-mail ao cadastro.
        String digitos = apenasDigitos(telefone);
        Long idDoEmail = cliente != null ? cliente.getId() : null;
        boolean telefoneEmOutroCadastro = clienteRepository.findAll().stream()
                .filter(c -> c.getTelefone() != null && !c.getTelefone().isBlank())
                .filter(c -> !c.getId().equals(idDoEmail))
                .anyMatch(c -> apenasDigitos(c.getTelefone()).equals(digitos));
        if (telefoneEmOutroCadastro) {
            throw new IllegalArgumentException(
                    "Esse telefone já está cadastrado na arena. Peça à recepção para vincular o seu e-mail ao cadastro e depois crie a conta.");
        }

        if (cliente == null) {
            cliente = Cliente.builder()
                    .nome(request.nome())
                    .email(request.email())
                    .telefone(telefone)
                    .origemCadastro("PORTAL")
                    .build();
        } else {
            cliente.setNome(request.nome());
            if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
                cliente.setTelefone(telefone);
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

    private static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
