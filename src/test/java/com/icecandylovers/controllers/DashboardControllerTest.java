package com.icecandylovers.controllers;

import com.icecandylovers.services.ProdutoService;
import com.icecandylovers.services.VendaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private ProdutoService produtoService;

    @Mock
    private VendaService vendaService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private DashboardController controller;

    @Test
    void showDashboard_deveRetornarViewComAtributos() {
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(produtoService.listarProdutos()).thenReturn(List.of());
        when(vendaService.obterVendasRecentes()).thenReturn(List.of());
        when(produtoService.calcularTotalEstoque()).thenReturn(100);
        when(vendaService.calcularVendasHoje()).thenReturn(BigDecimal.valueOf(500));

        Model model = new ConcurrentModel();
        String view = controller.showDashboard(model, request);

        assertEquals("dashboard", view);
        assertTrue(model.containsAttribute("produtos"));
        assertTrue(model.containsAttribute("vendasRecentes"));
        assertEquals(100, model.getAttribute("totalEstoque"));
        assertEquals(BigDecimal.valueOf(500), model.getAttribute("vendasHoje"));
    }

    @Test
    void showDashboard_deveRetornarError_quandoExcecao() {
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(produtoService.listarProdutos()).thenThrow(new RuntimeException("DB error"));

        Model model = new ConcurrentModel();
        String view = controller.showDashboard(model, request);

        assertEquals("error", view);
        assertTrue(model.containsAttribute("erro"));
    }
}
