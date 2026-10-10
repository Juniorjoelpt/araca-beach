package com.aracabeach.config;

import com.aracabeach.domain.usuario.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Quem esta logado na requisicao em andamento (equipe). Usado para gravar o operador nos recebimentos. */
public final class OperadorAtual {

    private OperadorAtual() {}

    /** Login do usuario da equipe logado, ou null (tarefa do sistema, portal do cliente etc.). */
    public static String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u) return u.getLogin();
        return null;
    }

    /** Nome (para exibicao) do usuario da equipe logado, ou null. */
    public static String nome() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u) return u.getNome();
        return null;
    }

    public static boolean garcom() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Usuario u && u.getPerfil().name().equals("GARCOM");
    }

    public static boolean admin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Usuario u && u.getPerfil().name().equals("ADMIN");
    }
}
