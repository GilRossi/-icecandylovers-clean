package com.icecandylovers.controllers;

import com.icecandylovers.services.ProdutoService;
import com.icecandylovers.services.VendaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Mock
    private ProdutoService produtoService;

    @Mock
    private VendaService vendaService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private DashboardController controller;

    @Nested
    @DisplayName("showDashboard")
    class ShowDashboard {

        @Test
        @DisplayName("deve retornar view com atributos básicos e novos KPIs")
        void showDashboard_deveRetornarViewComAtributos() {
            when(request.getRequestURI()).thenReturn("/dashboard");
            when(produtoService.listarProdutos()).thenReturn(List.of());
            when(vendaService.obterVendasRecentes()).thenReturn(List.of());
            when(produtoService.calcularTotalEstoque()).thenReturn(100);
            when(vendaService.calcularVendasHoje()).thenReturn(BigDecimal.valueOf(500));
            when(vendaService.calcularTotalVendasMes()).thenReturn(BigDecimal.ZERO);
            when(vendaService.calcularCrescimentoMensal()).thenReturn(null);
            when(vendaService.calcularTicketMedioMes()).thenReturn(BigDecimal.ZERO);
            when(produtoService.listarProdutosEstoqueBaixo()).thenReturn(List.of());
            when(vendaService.listarTop5ProdutosMaisVendidos()).thenReturn(List.of());

            Model model = new ConcurrentModel();
            String view = controller.showDashboard(model, request);

            assertEquals("dashboard", view);
            assertTrue(model.containsAttribute("produtos"));
            assertTrue(model.containsAttribute("vendasRecentes"));
            assertEquals(100, model.getAttribute("totalEstoque"));
            assertEquals(BigDecimal.valueOf(500), model.getAttribute("vendasHoje"));
            assertTrue(model.containsAttribute("totalVendasMes"));
            // crescimentoMensal pode ser null (sem dados do mês anterior); ConcurrentModel
            // não armazena chaves com valor null, portanto verificamos que o valor retornado é null
            assertNull(model.getAttribute("crescimentoMensal"));
            assertTrue(model.containsAttribute("ticketMedioMes"));
            assertTrue(model.containsAttribute("produtosEstoqueBaixo"));
            assertTrue(model.containsAttribute("top5Produtos"));
        }

        @Test
        @DisplayName("deve popular totalVendasMes e crescimentoMensal com valores reais")
        void showDashboard_devePopularKpisComValoresReais() {
            when(request.getRequestURI()).thenReturn("/dashboard");
            when(produtoService.listarProdutos()).thenReturn(List.of());
            when(vendaService.obterVendasRecentes()).thenReturn(List.of());
            when(produtoService.calcularTotalEstoque()).thenReturn(50);
            when(vendaService.calcularVendasHoje()).thenReturn(BigDecimal.valueOf(200));
            when(vendaService.calcularTotalVendasMes()).thenReturn(new BigDecimal("1500.00"));
            when(vendaService.calcularCrescimentoMensal()).thenReturn(25.0);
            when(vendaService.calcularTicketMedioMes()).thenReturn(new BigDecimal("75.00"));
            when(produtoService.listarProdutosEstoqueBaixo()).thenReturn(List.of());
            when(vendaService.listarTop5ProdutosMaisVendidos()).thenReturn(List.of());

            Model model = new ConcurrentModel();
            String view = controller.showDashboard(model, request);

            assertEquals("dashboard", view);
            assertEquals(new BigDecimal("1500.00"), model.getAttribute("totalVendasMes"));
            assertEquals(25.0, model.getAttribute("crescimentoMensal"));
            assertEquals(new BigDecimal("75.00"), model.getAttribute("ticketMedioMes"));
        }

        @Test
        @DisplayName("deve retornar error quando ocorre exceção")
        void showDashboard_deveRetornarError_quandoExcecao() {
            when(request.getRequestURI()).thenReturn("/dashboard");
            when(produtoService.listarProdutos()).thenThrow(new RuntimeException("DB error"));

            Model model = new ConcurrentModel();
            String view = controller.showDashboard(model, request);

            assertEquals("error", view);
            assertTrue(model.containsAttribute("erro"));
        }
    }
}
