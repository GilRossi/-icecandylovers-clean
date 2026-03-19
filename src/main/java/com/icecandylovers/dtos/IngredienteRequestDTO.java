package com.icecandylovers.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record IngredienteRequestDTO(
        @NotNull(message = "Os dados do ingrediente são obrigatórios.")
        @Valid
        IngredienteDTO ingrediente,
        @NotNull(message = "Os dados do lote são obrigatórios.")
        @Valid
        LoteDTO lote
) {}
