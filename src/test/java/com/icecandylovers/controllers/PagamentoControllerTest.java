package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.ProcessarPagamentoDTO;
import com.icecandylovers.dtos.ResultadoPagamentoDTO;
import com.icecandylovers.entities.StatusPagamento;
import com.icecandylovers.services.GatewayPagamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PagamentoController Unit Tests")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GatewayPagamentoService gatewayPagamentoService;

    @Nested
    @DisplayName("POST /api/pagamentos/processar")
    class ProcessarPagamento {

        @Test
        @DisplayName("deve retornar 200 e status APROVADO para pagamento aprovado")
        void deve_retornar_200_para_pagamento_aprovado() throws Exception {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("50.00"), "PIX", "Geladinho Morango");
            ResultadoPagamentoDTO resultado = new ResultadoPagamentoDTO(
                    "TXN-ABC12345", StatusPagamento.APROVADO,
                    "Pagamento aprovado com sucesso.", new BigDecimal("50.00"), LocalDateTime.now());

            when(gatewayPagamentoService.processarPagamento(any(ProcessarPagamentoDTO.class)))
                    .thenReturn(resultado);

            // when / then
            mockMvc.perform(post("/api/pagamentos/processar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADO"))
                    .andExpect(jsonPath("$.transacaoId").value("TXN-ABC12345"));
        }

        @Test
        @DisplayName("deve retornar 402 e status RECUSADO para pagamento recusado")
        void deve_retornar_402_para_pagamento_recusado() throws Exception {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("100.00"), "CARTAO_DEBITO", "Geladinho Limão");
            ResultadoPagamentoDTO resultado = new ResultadoPagamentoDTO(
                    "TXN-DEF67890", StatusPagamento.RECUSADO,
                    "Pagamento recusado pela operadora.", new BigDecimal("100.00"), LocalDateTime.now());

            when(gatewayPagamentoService.processarPagamento(any(ProcessarPagamentoDTO.class)))
                    .thenReturn(resultado);

            // when / then
            mockMvc.perform(post("/api/pagamentos/processar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.status").value("RECUSADO"));
        }

        @Test
        @DisplayName("deve retornar 200 e status PENDENTE para boleto")
        void deve_retornar_200_para_boleto_pendente() throws Exception {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("80.00"), "BOLETO", "Geladinho Chocolate");
            ResultadoPagamentoDTO resultado = new ResultadoPagamentoDTO(
                    "TXN-GHI11111", StatusPagamento.PENDENTE,
                    "Boleto gerado. Pagamento confirmado após compensação.",
                    new BigDecimal("80.00"), LocalDateTime.now());

            when(gatewayPagamentoService.processarPagamento(any(ProcessarPagamentoDTO.class)))
                    .thenReturn(resultado);

            // when / then
            mockMvc.perform(post("/api/pagamentos/processar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDENTE"));
        }

        @Test
        @DisplayName("deve retornar 400 quando valor é inválido")
        void deve_retornar_400_quando_valor_invalido() throws Exception {
            // given - valor = 0 falha @DecimalMin("0.01")
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    BigDecimal.ZERO, "PIX", "Produto teste");

            // when / then
            mockMvc.perform(post("/api/pagamentos/processar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando formaPagamento está em branco")
        void deve_retornar_400_quando_forma_pagamento_em_branco() throws Exception {
            // given - formaPagamento em branco falha @NotBlank
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("50.00"), "", "Produto teste");

            // when / then
            mockMvc.perform(post("/api/pagamentos/processar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }
}
