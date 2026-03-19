package com.icecandylovers.services;

import com.icecandylovers.dtos.ProcessarPagamentoDTO;
import com.icecandylovers.dtos.ResultadoPagamentoDTO;
import com.icecandylovers.entities.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayPagamentoMockService Unit Tests")
class GatewayPagamentoMockServiceTest {

    private GatewayPagamentoMockService service;

    @BeforeEach
    void setUp() {
        service = new GatewayPagamentoMockService();
        ReflectionTestUtils.setField(service, "simularRecusa", false);
    }

    @Nested
    @DisplayName("processarPagamento")
    class ProcessarPagamento {

        @Test
        @DisplayName("DINHEIRO deve retornar APROVADO sempre")
        void dinheiro_deve_retornar_aprovado() {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("50.00"), "DINHEIRO", "Geladinho Morango");

            // when
            ResultadoPagamentoDTO resultado = service.processarPagamento(dto);

            // then
            assertEquals(StatusPagamento.APROVADO, resultado.status());
            assertNotNull(resultado.transacaoId());
            assertTrue(resultado.transacaoId().startsWith("TXN-"));
            assertEquals(new BigDecimal("50.00"), resultado.valor());
            assertNotNull(resultado.processadoEm());
        }

        @Test
        @DisplayName("BOLETO deve retornar PENDENTE")
        void boleto_deve_retornar_pendente() {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("100.00"), "BOLETO", "Geladinho Limão");

            // when
            ResultadoPagamentoDTO resultado = service.processarPagamento(dto);

            // then
            assertEquals(StatusPagamento.PENDENTE, resultado.status());
            assertNotNull(resultado.transacaoId());
        }

        @Test
        @DisplayName("CARTAO_CREDITO deve retornar APROVADO quando simularRecusa=false")
        void cartao_credito_deve_retornar_aprovado() {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("75.00"), "CARTAO_CREDITO", "Geladinho Chocolate");

            // when
            ResultadoPagamentoDTO resultado = service.processarPagamento(dto);

            // then
            assertEquals(StatusPagamento.APROVADO, resultado.status());
        }

        @Test
        @DisplayName("deve retornar RECUSADO para qualquer forma quando simularRecusa=true")
        void deve_retornar_recusado_quando_simular_recusa_true() {
            // given
            ReflectionTestUtils.setField(service, "simularRecusa", true);
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("30.00"), "CARTAO_DEBITO", "Geladinho Uva");

            // when
            ResultadoPagamentoDTO resultado = service.processarPagamento(dto);

            // then
            assertEquals(StatusPagamento.RECUSADO, resultado.status());
            assertNotNull(resultado.transacaoId());
            assertEquals(new BigDecimal("30.00"), resultado.valor());
        }

        @Test
        @DisplayName("PIX deve retornar APROVADO quando simularRecusa=false")
        void pix_deve_retornar_aprovado() {
            // given
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("45.00"), "PIX", "Geladinho Tamarindo");

            // when
            ResultadoPagamentoDTO resultado = service.processarPagamento(dto);

            // then
            assertEquals(StatusPagamento.APROVADO, resultado.status());
        }

        @Test
        @DisplayName("DINHEIRO deve retornar APROVADO mesmo quando simularRecusa=true")
        void dinheiro_deve_retornar_aprovado_mesmo_com_simular_recusa() {
            // given
            ReflectionTestUtils.setField(service, "simularRecusa", true);
            ProcessarPagamentoDTO dto = new ProcessarPagamentoDTO(
                    new BigDecimal("20.00"), "DINHEIRO", "Geladinho Coco");

            // when
            ResultadoPagamentoDTO resultado = service.processarPagamento(dto);

            // then
            // DINHEIRO always succeeds regardless of simularRecusa
            assertEquals(StatusPagamento.APROVADO, resultado.status());
        }
    }
}
