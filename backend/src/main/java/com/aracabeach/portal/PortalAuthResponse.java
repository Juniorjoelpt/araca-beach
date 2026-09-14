package com.aracabeach.portal;

public record PortalAuthResponse(
        String token,
        Long clienteId,
        String nome,
        String email
) {
}
