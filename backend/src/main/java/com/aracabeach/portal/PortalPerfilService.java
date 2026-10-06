package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Perfil do jogador, troca de senha e recuperacao de senha por e-mail. */
@Service
@RequiredArgsConstructor
public class PortalPerfilService {

    private static final int VALIDADE_TOKEN_MINUTOS = 60;

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacaoService notificacaoService;

    @Value("${araca-beach.portal.url-base:http://localhost:5173}")
    private String urlBase;

    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public PortalPerfilResponse perfil(Cliente cliente) {
        Cliente atual = clienteRepository.findById(cliente.getId()).orElse(cliente);
        return paraResponse(atual);
    }

    @Transactional
    public PortalPerfilResponse atualizar(Cliente cliente, PortalPerfilRequest request) {
        Cliente atual = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cadastro não encontrado."));

        String telefone = request.telefone().trim();
        String digitos = apenasDigitos(telefone);
        if (digitos.length() < 10) {
            throw new IllegalArgumentException("Informe o telefone com DDD.");
        }
        boolean emUso = clienteRepository.findAll().stream()
                .filter(c -> !c.getId().equals(atual.getId()))
                .filter(c -> c.getTelefone() != null && !c.getTelefone().isBlank())
                .anyMatch(c -> apenasDigitos(c.getTelefone()).equals(digitos));
        if (emUso) {
            throw new IllegalArgumentException("Esse telefone já está cadastrado em outro cliente. Fale com a recepção.");
        }

        atual.setNome(request.nome().trim());
        atual.setTelefone(telefone);
        return paraResponse(clienteRepository.save(atual));
    }

    @Transactional
    public void alterarSenha(Cliente cliente, PortalAlterarSenhaRequest request) {
        Cliente atual = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cadastro não encontrado."));
        if (!passwordEncoder.matches(request.senhaAtual(), atual.getSenhaHash())) {
            throw new IllegalArgumentException("A senha atual está incorreta.");
        }
        atual.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        atual.setResetTokenHash(null);
        atual.setResetExpiraEm(null);
        clienteRepository.save(atual);
    }

    /**
     * Gera o link de recuperacao e envia por e-mail. Nunca revela se o e-mail
     * existe (resposta identica nos dois casos).
     */
    @Transactional
    public void esqueciSenha(String email) {
        clienteRepository.findByEmail(email.trim())
                .filter(Cliente::isPossuiAcessoPortal)
                .ifPresent(cliente -> {
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    cliente.setResetTokenHash(sha256(token));
                    cliente.setResetExpiraEm(LocalDateTime.now().plusMinutes(VALIDADE_TOKEN_MINUTOS));
                    clienteRepository.save(cliente);
                    String base = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
                    notificacaoService.enviarRecuperacaoSenha(cliente, base + "/portal/redefinir-senha?token=" + token);
                });
    }

    @Transactional
    public void redefinirSenha(PortalRedefinirSenhaRequest request) {
        Cliente cliente = clienteRepository.findByResetTokenHash(sha256(request.token()))
                .filter(c -> c.getResetExpiraEm() != null && c.getResetExpiraEm().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Link inválido ou expirado. Peça um novo em \"Esqueci minha senha\"."));
        cliente.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        cliente.setResetTokenHash(null);
        cliente.setResetExpiraEm(null);
        clienteRepository.save(cliente);
    }

    private PortalPerfilResponse paraResponse(Cliente c) {
        return new PortalPerfilResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone());
    }

    private static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private static String sha256(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
