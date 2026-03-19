package com.icecandylovers.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcessarPagamentoDTO(
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @NotBlank String formaPagamento,
        String produtoDescricao
) {}
