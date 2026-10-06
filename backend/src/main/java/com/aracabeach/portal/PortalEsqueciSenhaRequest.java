package com.aracabeach.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PortalEsqueciSenhaRequest(@NotBlank @Email String email) {
}
