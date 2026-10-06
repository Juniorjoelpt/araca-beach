package com.aracabeach.portal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public PortalRegistroResponse registrar(@Valid @RequestBody PortalRegistroRequest request) {
        return portalAuthService.registrar(request);
    }

    @PostMapping("/login")
    public PortalAuthResponse login(@Valid @RequestBody PortalLoginRequest request) {
        return portalAuthService.login(request);
    }

    @PostMapping("/confirmar-email")
    public PortalAuthResponse confirmarEmail(@Valid @RequestBody PortalConfirmarEmailRequest request) {
        return portalAuthService.confirmarEmail(request.token());
    }

    @PostMapping("/reenviar-confirmacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarConfirmacao(@Valid @RequestBody PortalEsqueciSenhaRequest request) {
        portalAuthService.reenviarConfirmacao(request.email());
    }
}
