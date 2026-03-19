package com.icecandylovers.services;

import com.icecandylovers.dtos.ProcessarPagamentoDTO;
import com.icecandylovers.dtos.ResultadoPagamentoDTO;

public interface GatewayPagamentoService {
    ResultadoPagamentoDTO processarPagamento(ProcessarPagamentoDTO dto);
}
