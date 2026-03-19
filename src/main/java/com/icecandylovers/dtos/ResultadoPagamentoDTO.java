package com.icecandylovers.dtos;

import com.icecandylovers.entities.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResultadoPagamentoDTO(
        String transacaoId,
        StatusPagamento status,
        String mensagem,
        BigDecimal valor,
        LocalDateTime processadoEm
) {}
