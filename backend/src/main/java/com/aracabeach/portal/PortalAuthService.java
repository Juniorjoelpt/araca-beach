package com.aracabeach.portal;

import com.aracabeach.config.JwtService;
import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import com.aracabeach.exception.EmailNaoConfirmadoException;
import com.aracabeach.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PortalAuthService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificacaoService notificacaoService;

    @Value("${araca-beach.portal.url-base:http://localhost:5173}")
    private String urlBase;

    @Value("${araca-beach.portal.exigir-confirmacao-email:true}")
    private boolean exigirConfirmacaoEmail;

    private static final int VALIDADE_CONFIRMACAO_HORAS = 24;

    /**
     * Cadastro do cliente no portal. Se ja existir um registro de Cliente
     * com esse e-mail (por exemplo, cadastrado antes pela recepcao) e ele
     * ainda nao tiver senha, "reivindicamos" esse registro em vez de criar
     * um duplicado - assim o cliente ja chega com o historico de reservas
     * anteriores vinculado a conta nova.
     */
    @Transactional
    public PortalRegistroResponse registrar(PortalRegistroRequest request) {
        Cliente cliente = clienteRepository.findByEmail(request.email()).orElse(null);

        // Conta ja confirmada: so login. Conta ainda NAO confirmada pode ser refeita (reenvia o link).
        if (cliente != null && cliente.isPossuiAcessoPortal() && !cliente.isEmailPendente()) {
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

        if (exigirConfirmacaoEmail) {
            // A senha so passa a valer depois que o dono do e-mail clicar no link: assim ninguem
            // assume o cadastro/historico de outra pessoa apenas digitando o e-mail dela.
            cliente.setEmailConfirmado(false);
            cliente = clienteRepository.save(cliente);
            enviarLinkConfirmacao(cliente);
            return new PortalRegistroResponse(true, cliente.getEmail(), null);
        }

        cliente.setEmailConfirmado(true);
        cliente = clienteRepository.save(cliente);
        return new PortalRegistroResponse(false, cliente.getEmail(), sessao(cliente));
    }

    /** Confirma o e-mail pelo link recebido e ja entrega a sessao (login automatico). */
    @Transactional
    public PortalAuthResponse confirmarEmail(String token) {
        Cliente cliente = clienteRepository.findByConfirmacaoTokenHash(TokenUtil.sha256(token))
                .filter(c -> c.getConfirmacaoExpiraEm() != null && c.getConfirmacaoExpiraEm().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Link inválido ou expirado. Tente entrar para receber um novo link de confirmação."));
        cliente.setEmailConfirmado(true);
        cliente.setConfirmacaoTokenHash(null);
        cliente.setConfirmacaoExpiraEm(null);
        clienteRepository.save(cliente);
        return sessao(cliente);
    }

    /** Reenvia o link de confirmacao. Resposta sempre igual, exista ou nao a conta. */
    @Transactional
    public void reenviarConfirmacao(String email) {
        clienteRepository.findByEmail(email.trim())
                .filter(Cliente::isEmailPendente)
                .ifPresent(this::enviarLinkConfirmacao);
    }

    private void enviarLinkConfirmacao(Cliente cliente) {
        String token = TokenUtil.gerar();
        cliente.setConfirmacaoTokenHash(TokenUtil.sha256(token));
        cliente.setConfirmacaoExpiraEm(LocalDateTime.now().plusHours(VALIDADE_CONFIRMACAO_HORAS));
        clienteRepository.save(cliente);
        String base = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        notificacaoService.enviarConfirmacaoEmail(cliente, base + "/portal/confirmar-email?token=" + token);
    }

    private PortalAuthResponse sessao(Cliente cliente) {
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
        if (cliente.isEmailPendente()) {
            throw new EmailNaoConfirmadoException(
                    "Confirme seu e-mail para entrar. Enviamos um link para " + cliente.getEmail() + ".");
        }

        String token = jwtService.gerarToken(cliente, JwtService.TIPO_CLIENTE);
        return new PortalAuthResponse(token, cliente.getId(), cliente.getNome(), cliente.getEmail());
    }

    private static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
