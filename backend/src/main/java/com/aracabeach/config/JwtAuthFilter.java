package com.aracabeach.config;

import com.aracabeach.repository.ClienteRepository;
import com.aracabeach.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica tanto tokens da equipe (Usuario) quanto tokens de clientes do
 * portal (Cliente), usando a claim "tipo" do JWT para decidir qual tabela
 * consultar. Um token de cliente nunca resolve para um Usuario e vice-versa,
 * mesmo que por coincidencia o "login"/e-mail exista nas duas tabelas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String login = jwtService.extrairLogin(token);
            String tipo = jwtService.extrairTipo(token);

            if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails principal = switch (tipo == null ? "" : tipo) {
                    case JwtService.TIPO_CLIENTE -> clienteRepository.findByEmail(login).orElse(null);
                    default -> usuarioRepository.findByLogin(login).orElse(null);
                };

                if (principal != null && jwtService.tokenValido(token, principal)) {
                    var authToken = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            // Token expirado, mal formado ou com assinatura invalida: nao autentica
            // e deixa o Spring Security recusar o acesso normalmente (401), em vez
            // de deixar a excecao vazar como erro 500. O frontend detecta o 401 e
            // redireciona para o login automaticamente.
            log.debug("Token JWT inválido ou expirado: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
