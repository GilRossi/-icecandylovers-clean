package com.icecandylovers.services;

import com.icecandylovers.dtos.ProcessarPagamentoDTO;
import com.icecandylovers.dtos.ResultadoPagamentoDTO;
import com.icecandylovers.entities.StatusPagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Primary
public class GatewayPagamentoMockService implements GatewayPagamentoService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayPagamentoMockService.class);

    @Value("${pagamento.gateway.simular-recusa:false}")
    private boolean simularRecusa;

    @Override
    public ResultadoPagamentoDTO processarPagamento(ProcessarPagamentoDTO dto) {
        logger.info("Processando pagamento mock: forma={}, valor={}", dto.formaPagamento(), dto.valor());

        String transacaoId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // DINHEIRO always succeeds
        if ("DINHEIRO".equalsIgnoreCase(dto.formaPagamento())) {
            return new ResultadoPagamentoDTO(transacaoId, StatusPagamento.APROVADO,
                    "Pagamento em dinheiro registrado com sucesso.", dto.valor(), LocalDateTime.now());
        }

        // Simulate decline if configured
        if (simularRecusa) {
            return new ResultadoPagamentoDTO(transacaoId, StatusPagamento.RECUSADO,
                    "Pagamento recusado pela operadora.", dto.valor(), LocalDateTime.now());
        }

        // BOLETO returns PENDENTE
        if ("BOLETO".equalsIgnoreCase(dto.formaPagamento())) {
            return new ResultadoPagamentoDTO(transacaoId, StatusPagamento.PENDENTE,
                    "Boleto gerado. Pagamento confirmado após compensação.", dto.valor(), LocalDateTime.now());
        }

        // All others: APROVADO
        return new ResultadoPagamentoDTO(transacaoId, StatusPagamento.APROVADO,
                "Pagamento aprovado com sucesso.", dto.valor(), LocalDateTime.now());
    }
}
