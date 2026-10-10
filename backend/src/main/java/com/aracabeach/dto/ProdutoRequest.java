package com.aracabeach.dto;

import com.aracabeach.domain.produto.CategoriaProduto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank String nome,
        @NotNull CategoriaProduto categoria,
        @NotNull @DecimalMin(value = "0.00") BigDecimal preco,
        Integer estoque,
        @DecimalMin(value = "0.00") BigDecimal custo,
        boolean ehAluguel
) {
}
