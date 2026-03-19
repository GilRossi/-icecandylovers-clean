package com.icecandylovers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LoteDTO(
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser maior que zero.")
        BigDecimal quantidade,
        @NotNull(message = "Custo por unidade é obrigatório.")
        @Positive(message = "Custo por unidade deve ser maior que zero.")
        BigDecimal custoPorUnidade
) {}