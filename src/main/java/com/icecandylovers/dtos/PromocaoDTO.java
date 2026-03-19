package com.icecandylovers.dtos;

import com.icecandylovers.entities.TipoDesconto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromocaoDTO(
        Long id,
        @NotBlank String nome,
        String descricao,
        @NotNull TipoDesconto tipo,
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        boolean ativo,
        List<Long> produtoIds
) {}
