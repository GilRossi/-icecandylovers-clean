package com.icecandylovers.dtos;

import com.icecandylovers.entities.CategoriaProduto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ProdutoDTO(
        Long id,
        @NotBlank(message = "O sabor é obrigatório.")
        String sabor,
        @NotNull(message = "O estoque inicial é obrigatório.")
        @Positive(message = "O estoque inicial deve ser maior que zero.")
        Integer estoqueInicial,
        @NotNull(message = "O estoque atual é obrigatório.")
        @PositiveOrZero(message = "O estoque atual não pode ser negativo.")
        Integer estoqueAtual,
        @NotNull(message = "O preço de custo é obrigatório.")
        @PositiveOrZero(message = "O preço de custo não pode ser negativo.")
        Double precoCusto,
        Double precoCustoUnitario,
        @NotNull(message = "A categoria é obrigatória.")
        CategoriaProduto categoria,
        @Valid
        List<ProdutoIngredienteDTO> ingredientes
) {}
