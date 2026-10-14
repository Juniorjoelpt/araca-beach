package com.aracabeach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModalidadeRequest(@NotBlank @Size(max = 60) String nome) {
}
