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

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.senha()));

        Usuario usuario = usuarioRepository.findByLogin(request.login())
                .orElseThrow();

        String token = jwtService.gerarToken(usuario, JwtService.TIPO_STAFF);
        return new LoginResponse(token, usuario.getNome(), usuario.getPerfil().name());
    }
}
