package com.icecandylovers.controllers;

import com.icecandylovers.dtos.ProcessarPagamentoDTO;
import com.icecandylovers.dtos.ResultadoPagamentoDTO;
import com.icecandylovers.entities.StatusPagamento;
import com.icecandylovers.services.GatewayPagamentoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoController.class);

    private final GatewayPagamentoService gatewayPagamentoService;

    public PagamentoController(GatewayPagamentoService gatewayPagamentoService) {
        this.gatewayPagamentoService = gatewayPagamentoService;
    }

    @PostMapping("/processar")
    public ResponseEntity<ResultadoPagamentoDTO> processarPagamento(@Valid @RequestBody ProcessarPagamentoDTO dto) {
        logger.info("Processando pagamento: forma={}, valor={}", dto.formaPagamento(), dto.valor());
        ResultadoPagamentoDTO resultado = gatewayPagamentoService.processarPagamento(dto);
        if (resultado.status() == StatusPagamento.RECUSADO) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(resultado);
        }
        return ResponseEntity.ok(resultado);
    }
}
