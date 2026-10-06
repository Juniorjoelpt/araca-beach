package com.aracabeach.service;

import com.aracabeach.config.JwtService;
import com.aracabeach.domain.usuario.Usuario;
import com.aracabeach.dto.LoginRequest;
import com.aracabeach.dto.LoginResponse;
import com.aracabeach.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login(), request.senha()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            auditoriaService.registrar("EQUIPE", request.login(), null, "Falha de login", "auth", null,
                    "Credenciais inválidas ou usuário inativo", ipAtual());
            throw e;
        }

        Usuario usuario = usuarioRepository.findByLogin(request.login())
                .orElseThrow();

        String token = jwtService.gerarToken(usuario, JwtService.TIPO_STAFF);
        auditoriaService.registrar("EQUIPE", usuario.getLogin(), usuario.getPerfil().name(), "Login", "auth",
                String.valueOf(usuario.getId()), null, ipAtual());
        return new LoginResponse(token, usuario.getNome(), usuario.getPerfil().name());
    }

    private String ipAtual() {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
            return com.aracabeach.config.AuditoriaInterceptor.ipDe(sra.getRequest());
        }
        return null;
    }
}
