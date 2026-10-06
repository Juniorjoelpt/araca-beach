package com.aracabeach.portal;

import com.aracabeach.domain.cliente.Cliente;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Perfil/senha do jogador. As rotas /auth/* sao publicas (ver SecurityConfig); as demais exigem login de cliente. */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalPerfilController {

    private final PortalPerfilService perfilService;

    @GetMapping("/perfil")
    public PortalPerfilResponse perfil(@AuthenticationPrincipal Cliente cliente) {
        return perfilService.perfil(cliente);
    }

    @PutMapping("/perfil")
    public PortalPerfilResponse atualizar(@AuthenticationPrincipal Cliente cliente,
                                          @Valid @RequestBody PortalPerfilRequest request) {
        return perfilService.atualizar(cliente, request);
    }

    @PatchMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@AuthenticationPrincipal Cliente cliente,
                             @Valid @RequestBody PortalAlterarSenhaRequest request) {
        perfilService.alterarSenha(cliente, request);
    }

    @PostMapping("/auth/esqueci-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void esqueciSenha(@Valid @RequestBody PortalEsqueciSenhaRequest request) {
        perfilService.esqueciSenha(request.email());
    }

    @PostMapping("/auth/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody PortalRedefinirSenhaRequest request) {
        perfilService.redefinirSenha(request);
    }
}
