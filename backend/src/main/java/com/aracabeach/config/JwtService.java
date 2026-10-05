package com.aracabeach.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Gera e valida tokens JWT tanto para a equipe (Usuario) quanto para
 * clientes logados no portal (Cliente). A claim "tipo" ("STAFF" ou
 * "CLIENTE") no token diz qual das duas tabelas o JwtAuthFilter deve
 * consultar, e tambem impede que um token de cliente seja usado para
 * acessar rotas internas da equipe (e vice-versa).
 */
@Service
public class JwtService {

    public static final String TIPO_STAFF = "STAFF";
    public static final String TIPO_CLIENTE = "CLIENTE";

    @Value("${araca-beach.jwt.secret}")
    private String secret;

    @Value("${araca-beach.jwt.expiration-minutes:480}")
    private long expiracaoMinutos;

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String gerarToken(UserDetails userDetails, String tipo) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMinutos * 60_000);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(Map.of("tipo", tipo))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave())
                .compact();
    }

    public String extrairLogin(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public String extrairTipo(String token) {
        return extrairClaim(token, claims -> claims.get("tipo", String.class));
    }

    /**
     * Alem de login/expiracao, verificamos userDetails.isEnabled(): sem isso,
     * um usuario desativado (Usuario.ativo = false) continuava autenticado
     * normalmente ate o token expirar (ate 8h depois de desativado), porque
     * o JwtAuthFilter monta o UsernamePasswordAuthenticationToken na mao e
     * nunca passa pelo AuthenticationManager/DaoAuthenticationProvider
     * (que e quem normalmente checa isEnabled()).
     */
    public boolean tokenValido(String token, UserDetails userDetails) {
        String login = extrairLogin(token);
        return login.equals(userDetails.getUsername())
                && !tokenExpirado(token)
                && userDetails.isEnabled();
    }

    private boolean tokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(chave()).build().parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }
}
