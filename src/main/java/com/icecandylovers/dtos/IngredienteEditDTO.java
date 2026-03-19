package com.icecandylovers.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record IngredienteEditDTO(
        @NotNull(message = "ID é obrigatório para edição.")
        Long id,
        @NotEmpty(message = "O campo 'nome' não pode estar vazio.")
        String nome,
        @NotNull(message = "Custo por unidade é obrigatório.")
        @PositiveOrZero(message = "Custo por unidade não pode ser negativo.")
        BigDecimal custoPorUnidade,
        @NotEmpty(message = "Unidade de medida é obrigatória.")
        String unidadeMedida,
        @NotNull(message = "Estoque atual é obrigatório.")
        @PositiveOrZero(message = "Estoque atual não pode ser negativo.")
        BigDecimal estoqueAtual
) {}