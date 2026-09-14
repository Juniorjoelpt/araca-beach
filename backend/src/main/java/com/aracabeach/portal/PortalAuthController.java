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
    public PortalAuthResponse registrar(@Valid @RequestBody PortalRegistroRequest request) {
        return portalAuthService.registrar(request);
    }

    @PostMapping("/login")
    public PortalAuthResponse login(@Valid @RequestBody PortalLoginRequest request) {
        return portalAuthService.login(request);
    }
}
