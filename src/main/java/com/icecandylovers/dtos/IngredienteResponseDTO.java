package com.icecandylovers.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.icecandylovers.entities.Ingrediente;

import java.math.BigDecimal;

public record IngredienteResponseDTO(
        Long id,
        String nome,
        String unidadeMedida,
        @JsonFormat(shape = JsonFormat.Shape.NUMBER) // Força serialização numérica
        BigDecimal estoqueAtual,
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        BigDecimal custoPorUnidade
) {
    public IngredienteResponseDTO(Ingrediente ingrediente) {
        this(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                ingrediente.getEstoqueAtual() != null ? ingrediente.getEstoqueAtual() : BigDecimal.ZERO,
                ingrediente.getCustoPorUnidade() != null ? ingrediente.getCustoPorUnidade() : BigDecimal.ZERO
        );
    }
}
