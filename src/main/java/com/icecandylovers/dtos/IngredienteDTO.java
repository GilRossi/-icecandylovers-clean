package com.icecandylovers.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record IngredienteDTO(
        Long id,  // Para suportar ingredientes existentes
        @NotEmpty(message = "O campo 'nome' não pode estar vazio.")
        String nome,
        @NotNull(message = "Custo por unidade é obrigatório.")
        @PositiveOrZero(message = "Custo por unidade não pode ser negativo.")
        BigDecimal custoPorUnidade,
        @NotEmpty(message = "Unidade de medida é obrigatória.")
        String unidadeMedida,
        @NotNull(message = "Estoque inicial é obrigatório.")
        @PositiveOrZero(message = "Estoque inicial não pode ser negativo.")
        BigDecimal estoqueInicial,
        @NotNull(message = "Estoque atual é obrigatório.")
        @PositiveOrZero(message = "Estoque atual não pode ser negativo.")
        BigDecimal estoqueAtual  // Adicionado para suportar o estoque atual
) {}