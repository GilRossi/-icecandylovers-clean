package com.icecandylovers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProdutoIngredienteDTO(
        @NotNull(message = "O ingrediente é obrigatório.")
        Long ingredienteId,
        String nome,
        @Positive(message = "A quantidade deve ser maior que zero.")
        double quantidade
) {}
