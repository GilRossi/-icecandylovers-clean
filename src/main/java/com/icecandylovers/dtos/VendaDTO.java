package com.icecandylovers.dtos;

import com.icecandylovers.entities.Vendido;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendaDTO(
        @NotNull(message = "O ID do produto é obrigatório!")
        Long produtoId,
        @NotNull(message = "A quantidade é obrigatória!")
        @Positive(message = "A quantidade deve ser maior que zero!")
        Integer quantidade,
        @NotNull(message = "O canal da venda é obrigatório!")
        Vendido vendido,
        @NotNull(message = "O valor unitário é obrigatório!")
        @DecimalMin(value = "0.01", message = "O valor unitário deve ser maior que zero!")
        BigDecimal valorUnitarioVenda,
        @NotBlank(message = "A forma de pagamento é obrigatória!")
        String formaPagamento,
        @NotNull(message = "A data da venda é obrigatória!")
        LocalDateTime dataVenda
) {}
