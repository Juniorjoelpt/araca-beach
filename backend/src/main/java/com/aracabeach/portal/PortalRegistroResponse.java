package com.aracabeach.portal;

/**
 * confirmacaoPendente = true: conta criada, mas o jogador precisa confirmar o e-mail antes de
 * entrar (sessao nula). false: confirmacao desligada, sessao ja devolvida.
 */
public record PortalRegistroResponse(boolean confirmacaoPendente, String email, PortalAuthResponse sessao) {
}
