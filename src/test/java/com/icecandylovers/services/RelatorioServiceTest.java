package com.icecandylovers.services;

import com.icecandylovers.entities.Vendido;
import com.icecandylovers.repositories.ProdutoRepository;
import com.icecandylovers.repositories.VendaItemRepository;
import com.icecandylovers.repositories.VendaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RelatorioService Unit Tests")
class RelatorioServiceTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private VendaItemRepository vendaItemRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    private final LocalDateTime START = LocalDateTime.of(2025, 1, 1, 0, 0);
    private final LocalDateTime END = LocalDateTime.of(2025, 1, 31, 23, 59, 59);

    /**
     * Helper: set up all repository mocks so gerarRelatorioCompleto can run
     * without NullPointerExceptions. Individual tests override specific mocks.
     */
    private void setupDefaultMocks(Vendido canal) {
        // -- calcularTotalVendas --
        if (canal == Vendido.ALL) {
            when(vendaRepository.sumTotalVendasByPeriod(any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(vendaRepository.countVendasByPeriod(any(), any()))
                    .thenReturn(0L);
            when(vendaRepository.sumCustosByPeriod(any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(vendaRepository.findVendasAgrupadasPorFormaPagamento(any(), any()))
                    .thenReturn(new ArrayList<>());
        } else {
            when(vendaRepository.sumTotalVendasByPeriodAndChannel(any(), any(), eq(canal)))
                    .thenReturn(BigDecimal.ZERO);
            when(vendaRepository.countVendasByPeriodAndChannel(any(), any(), eq(canal)))
                    .thenReturn(0L);
            when(vendaRepository.sumCustosByPeriodAndChannel(any(), any(), eq(canal)))
                    .thenReturn(BigDecimal.ZERO);
            when(vendaRepository.findVendasAgrupadasPorFormaPagamentoAndChannel(any(), any(), eq(canal)))
                    .thenReturn(new ArrayList<>());
        }

        // -- shared queries (always called) --
        when(vendaRepository.findVendasAgrupadasPorPeriodo(any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(vendaRepository.findTopProdutos(any(), any(), any(), eq(Integer.MAX_VALUE)))
                .thenReturn(new ArrayList<>());

        when(vendaItemRepository.findVendasAgrupadasPorCanal(any(), any()))
                .thenReturn(new ArrayList<>());
        when(vendaItemRepository.findPrecoCustoUnitarioPorProduto(any(), any()))
                .thenReturn(new ArrayList<>());
        when(vendaItemRepository.findIngredientesComPrecoEEstoque(any(), any()))
                .thenReturn(new ArrayList<>());

        when(produtoRepository.findQuantidadeEstoquePorProduto())
                .thenReturn(new ArrayList<>());
    }

    // =========================================================================
    // gerarRelatorioCompleto
    // =========================================================================
    @Nested
    @DisplayName("gerarRelatorioCompleto")
    class GerarRelatorioCompleto {

        @Test
        @DisplayName("should generate complete report with all fields")
        void shouldGenerateCompleteReportWithAllFields() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            // Override with meaningful data
            when(vendaRepository.sumTotalVendasByPeriod(START, END))
                    .thenReturn(new BigDecimal("5000.00"));
            when(vendaRepository.countVendasByPeriod(START, END))
                    .thenReturn(100L);
            when(vendaRepository.sumCustosByPeriod(START, END))
                    .thenReturn(new BigDecimal("2000.00"));

            List<Object[]> vendasPorPeriodo = List.of(
                    new Object[]{"2025-01-01", new BigDecimal("500.00")},
                    new Object[]{"2025-01-02", new BigDecimal("600.00")}
            );
            when(vendaRepository.findVendasAgrupadasPorPeriodo(START, END, canal))
                    .thenReturn(vendasPorPeriodo);

            List<Object[]> topProdutos = List.of(
                    new Object[]{"Morango", 50L},
                    new Object[]{"Chocolate", 30L}
            );
            when(vendaRepository.findTopProdutos(START, END, canal, Integer.MAX_VALUE))
                    .thenReturn(topProdutos);

            List<Object[]> estoqueProduto = List.of(
                    new Object[]{"Morango", 25},
                    new Object[]{"Chocolate", 10}
            );
            when(produtoRepository.findQuantidadeEstoquePorProduto()).thenReturn(estoqueProduto);

            List<Object[]> vendasPorCanal = List.of(
                    new Object[]{Vendido.PRAIA, new BigDecimal("3000.00")},
                    new Object[]{Vendido.EVENTO, new BigDecimal("2000.00")}
            );
            when(vendaItemRepository.findVendasAgrupadasPorCanal(START, END))
                    .thenReturn(vendasPorCanal);

            List<Object[]> precoCusto = new ArrayList<>();
            precoCusto.add(new Object[]{"Morango", new BigDecimal("4.50")});
            when(vendaItemRepository.findPrecoCustoUnitarioPorProduto(START, END))
                    .thenReturn(precoCusto);

            List<Object[]> ingredientesRaw = new ArrayList<>();
            ingredientesRaw.add(new Object[]{"Acucar", new BigDecimal("3.50"), new BigDecimal("100")});
            when(vendaItemRepository.findIngredientesComPrecoEEstoque(START, END))
                    .thenReturn(ingredientesRaw);

            List<Object[]> formaPagamento = List.of(
                    new Object[]{"PIX", new BigDecimal("3000.00")},
                    new Object[]{"DINHEIRO", new BigDecimal("2000.00")}
            );
            when(vendaRepository.findVendasAgrupadasPorFormaPagamento(START, END))
                    .thenReturn(formaPagamento);

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            // Verify all expected keys are present
            assertNotNull(relatorio);
            assertTrue(relatorio.containsKey("totalVendas"));
            assertTrue(relatorio.containsKey("ticketMedio"));
            assertTrue(relatorio.containsKey("clientesUnicos"));
            assertTrue(relatorio.containsKey("vendasPorPeriodo"));
            assertTrue(relatorio.containsKey("produtosMaisVendidos"));
            assertTrue(relatorio.containsKey("quantidadeEstoquePorProduto"));
            assertTrue(relatorio.containsKey("vendasPorCanal"));
            assertTrue(relatorio.containsKey("precoCustoUnitario"));
            assertTrue(relatorio.containsKey("ingredientes"));
            assertTrue(relatorio.containsKey("vendasPorFormaPagamento"));
            assertTrue(relatorio.containsKey("taxaConversao"));
            assertTrue(relatorio.containsKey("margemLucroBruto"));
            assertTrue(relatorio.containsKey("kpiData"));

            // Verify values
            assertEquals(5000.00, (Double) relatorio.get("totalVendas"), 0.01);
            // ticket medio = 5000 / 100 = 50.00
            assertEquals(50.00, (Double) relatorio.get("ticketMedio"), 0.01);
            assertEquals(100, relatorio.get("clientesUnicos"));
            assertEquals(vendasPorPeriodo, relatorio.get("vendasPorPeriodo"));
            assertEquals(topProdutos, relatorio.get("produtosMaisVendidos"));
            assertEquals(estoqueProduto, relatorio.get("quantidadeEstoquePorProduto"));
            assertEquals(vendasPorCanal, relatorio.get("vendasPorCanal"));
            assertEquals(precoCusto, relatorio.get("precoCustoUnitario"));
            assertEquals(formaPagamento, relatorio.get("vendasPorFormaPagamento"));

            // ingredientes should be transformed into List<Map>
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ingredientes = (List<Map<String, Object>>) relatorio.get("ingredientes");
            assertEquals(1, ingredientes.size());
            assertEquals("Acucar", ingredientes.get(0).get("nome"));
            assertEquals(new BigDecimal("3.50"), ingredientes.get(0).get("preco"));
            assertEquals(new BigDecimal("100"), ingredientes.get(0).get("estoque"));

            // KPI data
            @SuppressWarnings("unchecked")
            Map<String, Object> kpiData = (Map<String, Object>) relatorio.get("kpiData");
            assertNotNull(kpiData);
            assertTrue(kpiData.containsKey("taxaConversao"));
            assertTrue(kpiData.containsKey("margemLucroBruto"));
        }

        @Test
        @DisplayName("should handle Vendido.ALL channel (uses non-channel-specific queries)")
        void shouldHandleVendidoAllChannel() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            when(vendaRepository.sumTotalVendasByPeriod(START, END))
                    .thenReturn(new BigDecimal("1000.00"));
            when(vendaRepository.countVendasByPeriod(START, END)).thenReturn(10L);
            when(vendaRepository.sumCustosByPeriod(START, END))
                    .thenReturn(new BigDecimal("400.00"));

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            assertNotNull(relatorio);

            // Verify the ALL-specific (non-channel) methods were called
            verify(vendaRepository, atLeastOnce()).sumTotalVendasByPeriod(START, END);
            verify(vendaRepository, atLeastOnce()).countVendasByPeriod(START, END);
            verify(vendaRepository).sumCustosByPeriod(START, END);
            verify(vendaRepository).findVendasAgrupadasPorFormaPagamento(START, END);

            // Verify channel-specific methods were NOT called
            verify(vendaRepository, never()).sumTotalVendasByPeriodAndChannel(any(), any(), any());
            verify(vendaRepository, never()).countVendasByPeriodAndChannel(any(), any(), any());
            verify(vendaRepository, never()).sumCustosByPeriodAndChannel(any(), any(), any());
            verify(vendaRepository, never()).findVendasAgrupadasPorFormaPagamentoAndChannel(any(), any(), any());
        }

        @Test
        @DisplayName("should handle specific channel (uses channel-specific queries)")
        void shouldHandleSpecificChannel() {
            Vendido canal = Vendido.PRAIA;
            setupDefaultMocks(canal);

            when(vendaRepository.sumTotalVendasByPeriodAndChannel(START, END, canal))
                    .thenReturn(new BigDecimal("2500.00"));
            when(vendaRepository.countVendasByPeriodAndChannel(START, END, canal))
                    .thenReturn(50L);
            when(vendaRepository.sumCustosByPeriodAndChannel(START, END, canal))
                    .thenReturn(new BigDecimal("1000.00"));

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            assertNotNull(relatorio);
            assertEquals(2500.00, (Double) relatorio.get("totalVendas"), 0.01);

            // Verify channel-specific methods were called
            verify(vendaRepository, atLeastOnce()).sumTotalVendasByPeriodAndChannel(START, END, canal);
            verify(vendaRepository, atLeastOnce()).countVendasByPeriodAndChannel(START, END, canal);
            verify(vendaRepository).sumCustosByPeriodAndChannel(START, END, canal);
            verify(vendaRepository).findVendasAgrupadasPorFormaPagamentoAndChannel(START, END, canal);

            // Verify non-channel methods were NOT called
            verify(vendaRepository, never()).sumTotalVendasByPeriod(any(), any());
            verify(vendaRepository, never()).countVendasByPeriod(any(), any());
            verify(vendaRepository, never()).sumCustosByPeriod(any(), any());
            verify(vendaRepository, never()).findVendasAgrupadasPorFormaPagamento(any(), any());
        }

        @Test
        @DisplayName("should return zero metrics when no sales exist")
        void shouldReturnZeroMetricsWhenNoSalesExist() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            // All defaults are already zero/empty from setupDefaultMocks

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            assertNotNull(relatorio);
            assertEquals(0.0, (Double) relatorio.get("totalVendas"), 0.01);
            assertEquals(0.0, (Double) relatorio.get("ticketMedio"), 0.01);
            assertEquals(0, relatorio.get("clientesUnicos"));
            assertEquals(0.0, (Double) relatorio.get("taxaConversao"), 0.01);
            assertEquals(0.0, (Double) relatorio.get("margemLucroBruto"), 0.01);

            @SuppressWarnings("unchecked")
            List<Object[]> vendasPorPeriodo = (List<Object[]>) relatorio.get("vendasPorPeriodo");
            assertTrue(vendasPorPeriodo.isEmpty());

            @SuppressWarnings("unchecked")
            List<Object[]> produtosMaisVendidos = (List<Object[]>) relatorio.get("produtosMaisVendidos");
            assertTrue(produtosMaisVendidos.isEmpty());
        }

        @Test
        @DisplayName("should calculate ticket medio correctly (totalVendas / countVendas)")
        void shouldCalculateTicketMedioCorrectly() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            // Total vendas = 750.00, count = 15 -> ticket medio = 750/15 = 50.00
            when(vendaRepository.sumTotalVendasByPeriod(START, END))
                    .thenReturn(new BigDecimal("750.00"));
            when(vendaRepository.countVendasByPeriod(START, END)).thenReturn(15L);
            when(vendaRepository.sumCustosByPeriod(START, END))
                    .thenReturn(new BigDecimal("300.00"));

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            Double ticketMedio = (Double) relatorio.get("ticketMedio");
            assertEquals(50.00, ticketMedio, 0.01,
                    "Ticket medio should be 750 / 15 = 50.00");
        }

        @Test
        @DisplayName("should calculate ticket medio with non-round division")
        void shouldCalculateTicketMedioWithNonRoundDivision() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            // Total = 1000.00, count = 3 -> ticket medio = 333.33
            when(vendaRepository.sumTotalVendasByPeriod(START, END))
                    .thenReturn(new BigDecimal("1000.00"));
            when(vendaRepository.countVendasByPeriod(START, END)).thenReturn(3L);
            when(vendaRepository.sumCustosByPeriod(START, END))
                    .thenReturn(new BigDecimal("400.00"));

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            Double ticketMedio = (Double) relatorio.get("ticketMedio");
            // 1000 / 3 = 333.33 (HALF_UP)
            BigDecimal expected = new BigDecimal("1000.00")
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            assertEquals(expected.doubleValue(), ticketMedio, 0.01);
        }

        @Test
        @DisplayName("should calculate margem lucro correctly ((receita - custos) / receita * 100)")
        void shouldCalculateMargemLucroCorrectly() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            // Receita = 1000, Custos = 400
            // Margem = (1000 - 400) / 1000 * 100 = 60.00%
            when(vendaRepository.sumTotalVendasByPeriod(START, END))
                    .thenReturn(new BigDecimal("1000.00"));
            when(vendaRepository.countVendasByPeriod(START, END)).thenReturn(20L);
            when(vendaRepository.sumCustosByPeriod(START, END))
                    .thenReturn(new BigDecimal("400.00"));

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            Double margemLucro = (Double) relatorio.get("margemLucroBruto");
            // lucro = 1000 - 400 = 600
            // margem = (600 / 1000) rounded to 2 decimal places = 0.60 -> * 100 = 60.0
            assertEquals(60.0, margemLucro, 0.1,
                    "Margem lucro should be (1000 - 400) / 1000 * 100 = 60%");
        }

        @Test
        @DisplayName("should calculate margem lucro with higher cost ratio")
        void shouldCalculateMargemLucroWithHigherCostRatio() {
            Vendido canal = Vendido.EVENTO;
            setupDefaultMocks(canal);

            // Receita = 3000, Custos = 2100
            // Margem = (3000 - 2100) / 3000 * 100 = 900/3000 * 100 = 0.30 * 100 = 30%
            when(vendaRepository.sumTotalVendasByPeriodAndChannel(START, END, canal))
                    .thenReturn(new BigDecimal("3000.00"));
            when(vendaRepository.countVendasByPeriodAndChannel(START, END, canal))
                    .thenReturn(60L);
            when(vendaRepository.sumCustosByPeriodAndChannel(START, END, canal))
                    .thenReturn(new BigDecimal("2100.00"));

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            Double margemLucro = (Double) relatorio.get("margemLucroBruto");
            assertEquals(30.0, margemLucro, 0.1,
                    "Margem lucro should be (3000 - 2100) / 3000 * 100 = 30%");
        }

        @Test
        @DisplayName("should include estoque status in report")
        void shouldIncludeEstoqueStatusInReport() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            List<Object[]> estoqueProdutos = List.of(
                    new Object[]{"Morango", 25},
                    new Object[]{"Chocolate", 10},
                    new Object[]{"Limao", 0}
            );
            when(produtoRepository.findQuantidadeEstoquePorProduto()).thenReturn(estoqueProdutos);

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            // Verify quantidadeEstoquePorProduto is included and contains the data
            assertTrue(relatorio.containsKey("quantidadeEstoquePorProduto"));
            @SuppressWarnings("unchecked")
            List<Object[]> result = (List<Object[]>) relatorio.get("quantidadeEstoquePorProduto");
            assertEquals(3, result.size());

            // Verify the repository method was called
            verify(produtoRepository).findQuantidadeEstoquePorProduto();
        }

        @Test
        @DisplayName("should handle null return from sumTotalVendasByPeriod gracefully")
        void shouldHandleNullReturnFromSumTotalVendasGracefully() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            // sumTotalVendasByPeriod returns null (no vendas at all)
            when(vendaRepository.sumTotalVendasByPeriod(START, END)).thenReturn(null);

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            assertEquals(0.0, (Double) relatorio.get("totalVendas"), 0.01,
                    "Total vendas should default to 0 when repository returns null");
        }

        @Test
        @DisplayName("should handle null return from sumCustosByPeriod gracefully")
        void shouldHandleNullReturnFromSumCustosGracefully() {
            Vendido canal = Vendido.ALL;
            setupDefaultMocks(canal);

            when(vendaRepository.sumTotalVendasByPeriod(START, END))
                    .thenReturn(new BigDecimal("500.00"));
            when(vendaRepository.countVendasByPeriod(START, END)).thenReturn(10L);
            // custos returns null
            when(vendaRepository.sumCustosByPeriod(START, END)).thenReturn(null);

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(START, END, canal);

            // margem lucro: (500 - 0) / 500 * 100 = 100%
            Double margemLucro = (Double) relatorio.get("margemLucroBruto");
            assertEquals(100.0, margemLucro, 0.1,
                    "When custos is null (treated as 0), margem should be 100%");
        }
    }

    // =========================================================================
    // getTempoReposicao
    // =========================================================================
    @Nested
    @DisplayName("getTempoReposicao")
    class GetTempoReposicao {

        @Test
        @DisplayName("should return 0 (pending implementation)")
        void shouldReturnZeroPendingImplementation() {
            Integer resultado = relatorioService.getTempoReposicao();

            assertNotNull(resultado);
            assertEquals(0, resultado);
        }
    }
}
