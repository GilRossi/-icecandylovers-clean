package com.icecandylovers.controllers;

import com.icecandylovers.entities.Vendido;
import com.icecandylovers.services.RelatorioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioControllerTest {

    @Mock
    private RelatorioService relatorioService;

    @InjectMocks
    private RelatorioController controller;

    @Test
    void relatorios_deveRetornarViewRelatorios() {
        when(relatorioService.gerarRelatorioCompleto(any(), any(), any()))
                .thenReturn(Map.of("totalVendas", 1000.0));

        Model model = new ConcurrentModel();
        String view = controller.relatorios(model);

        assertEquals("relatorios", view);
        assertTrue(model.containsAttribute("totalVendas"));
    }

    @Test
    void getRelatorio_deveRetornarJson() {
        Map<String, Object> report = Map.of("totalVendas", 5000.0, "ticketMedio", 50.0);
        when(relatorioService.gerarRelatorioCompleto(any(), any(), any())).thenReturn(report);

        ResponseEntity<?> response = controller.getRelatorio(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 19), Vendido.ALL);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(5000.0, body.get("totalVendas"));
    }

    @Test
    void getRelatorio_deveRetornar500_quandoErro() {
        when(relatorioService.gerarRelatorioCompleto(any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = controller.getRelatorio(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 19), Vendido.ALL);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void getRelatorio_deveUsarCanalEspecifico() {
        Map<String, Object> report = Map.of("totalVendas", 2000.0);
        when(relatorioService.gerarRelatorioCompleto(any(), any(), any())).thenReturn(report);

        ResponseEntity<?> response = controller.getRelatorio(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 19), Vendido.PRAIA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
