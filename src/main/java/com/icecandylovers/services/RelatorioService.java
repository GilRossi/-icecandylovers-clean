package com.icecandylovers.services;

import com.icecandylovers.entities.Vendido;
import com.icecandylovers.repositories.ProdutoRepository;
import com.icecandylovers.repositories.VendaRepository;
import com.icecandylovers.repositories.VendaItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private static final Logger logger = LoggerFactory.getLogger(RelatorioService.class);

    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final ProdutoRepository produtoRepository;

    @Autowired
    public RelatorioService(VendaRepository vendaRepository,
                            VendaItemRepository vendaItemRepository,
                            ProdutoRepository produtoRepository) {
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.produtoRepository = produtoRepository;
    }

    private BigDecimal calcularTotalVendas(LocalDateTime start, LocalDateTime end, Vendido canal) {
        BigDecimal total = (canal == Vendido.ALL)
                ? vendaRepository.sumTotalVendasByPeriod(start, end)
                : vendaRepository.sumTotalVendasByPeriodAndChannel(start, end, canal);
        return total != null ? total : BigDecimal.ZERO;
    }

    private Double calcularTicketMedio(LocalDateTime start, LocalDateTime end, Vendido canal) {
        BigDecimal total = calcularTotalVendas(start, end, canal);
        Long quantidade = (canal == Vendido.ALL)
                ? vendaRepository.countVendasByPeriod(start, end)
                : vendaRepository.countVendasByPeriodAndChannel(start, end, canal);

        if (quantidade == null || quantidade <= 0 || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private Integer contarClientesUnicos(LocalDateTime start, LocalDateTime end, Vendido canal) {
        Long count = (canal == Vendido.ALL)
                ? vendaRepository.countVendasByPeriod(start, end)
                : vendaRepository.countVendasByPeriodAndChannel(start, end, canal);
        return count != null ? count.intValue() : 0;
    }

    private Double calcularTaxaConversao(LocalDateTime start, LocalDateTime end, Vendido canal) {
        Long visitas = 0L;
        Long vendas = (canal == Vendido.ALL)
                ? vendaRepository.countVendasByPeriod(start, end)
                : vendaRepository.countVendasByPeriodAndChannel(start, end, canal);
        if (vendas == null || visitas == 0) {
            return 0.0;
        }
        return (vendas.doubleValue() / visitas) * 100;
    }

    private Double calcularMargemLucro(LocalDateTime start, LocalDateTime end, Vendido canal) {
        BigDecimal custos = (canal == Vendido.ALL)
                ? vendaRepository.sumCustosByPeriod(start, end)
                : vendaRepository.sumCustosByPeriodAndChannel(start, end, canal);
        BigDecimal receita = calcularTotalVendas(start, end, canal);

        custos = custos != null ? custos : BigDecimal.ZERO;
        if (receita.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        BigDecimal lucro = receita.subtract(custos);
        return lucro.divide(receita, 2, RoundingMode.HALF_UP).doubleValue() * 100;
    }

    private Map<String, Integer> calcularStatusEstoque() {
        Map<String, Integer> estoque = new HashMap<>();
        estoque.put("EM_ESTOQUE", vendaRepository.countProdutosComEstoque());
        estoque.put("BAIXO_ESTOQUE", vendaRepository.countProdutosComEstoqueBaixo());
        estoque.put("SEM_ESTOQUE", vendaRepository.countProdutosSemEstoque());
        return estoque;
    }

    private List<Object[]> calcularVendasPorCanal(LocalDateTime start, LocalDateTime end) {
        return vendaItemRepository.findVendasAgrupadasPorCanal(start, end);
    }

    private List<Object[]> calcularPrecoCustoUnitario(LocalDateTime start, LocalDateTime end) {
        return vendaItemRepository.findPrecoCustoUnitarioPorProduto(start, end);
    }

    private List<Map<String, Object>> calcularIngredientes(LocalDateTime start, LocalDateTime end) {
        List<Object[]> ingredientesRaw = vendaItemRepository.findIngredientesComPrecoEEstoque(start, end);
        List<Map<String, Object>> ingredientes = new ArrayList<>();
        for (Object[] row : ingredientesRaw) {
            Map<String, Object> ingrediente = new HashMap<>();
            ingrediente.put("nome", row[0]); // String
            ingrediente.put("preco", row[1]); // BigDecimal
            ingrediente.put("estoque", row[2]); // BigDecimal
            ingredientes.add(ingrediente);
        }
        return ingredientes;
    }

    private List<Object[]> calcularQuantidadeEstoquePorProduto() {
        List<Object[]> result = produtoRepository.findQuantidadeEstoquePorProduto();
        logger.debug("QuantidadeEstoquePorProduto carregada: {}", result != null ? result.size() : 0);
        return result != null ? result : new ArrayList<>();
    }

    public Integer getTempoReposicao() {
        return 0; // Implementação pendente
    }

    public Map<String, Object> gerarRelatorioCompleto(LocalDateTime startDate, LocalDateTime endDate, Vendido salesChannel) {
        Map<String, Object> relatorio = new HashMap<>();

        try {
            // Métricas básicas
            BigDecimal totalVendas = calcularTotalVendas(startDate, endDate, salesChannel);
            Double ticketMedio = calcularTicketMedio(startDate, endDate, salesChannel);
            Integer clientesUnicos = contarClientesUnicos(startDate, endDate, salesChannel);

            // Dados para gráficos
            List<Object[]> vendasPorPeriodo = vendaRepository.findVendasAgrupadasPorPeriodo(startDate, endDate, salesChannel);
            List<Object[]> produtosMaisVendidos = vendaRepository.findTopProdutos(startDate, endDate, salesChannel, Integer.MAX_VALUE);
            List<Object[]> quantidadeEstoquePorProduto = calcularQuantidadeEstoquePorProduto();
            List<Object[]> vendasPorCanal = calcularVendasPorCanal(startDate, endDate);
            List<Object[]> precoCustoUnitario = calcularPrecoCustoUnitario(startDate, endDate);
            List<Map<String, Object>> ingredientes = calcularIngredientes(startDate, endDate);
            List<Object[]> vendasPorFormaPagamento = calcularVendasPorFormaPagamento(startDate, endDate, salesChannel); // Novo

            // KPIs
            Double taxaConversao = calcularTaxaConversao(startDate, endDate, salesChannel);
            Double margemLucroBruto = calcularMargemLucro(startDate, endDate, salesChannel);

            // Adiciona os dados ao relatório
            relatorio.put("totalVendas", totalVendas.doubleValue());
            relatorio.put("ticketMedio", ticketMedio);
            relatorio.put("clientesUnicos", clientesUnicos);
            relatorio.put("vendasPorPeriodo", vendasPorPeriodo);
            relatorio.put("produtosMaisVendidos", produtosMaisVendidos);
            relatorio.put("quantidadeEstoquePorProduto", quantidadeEstoquePorProduto);
            relatorio.put("vendasPorCanal", vendasPorCanal);
            relatorio.put("precoCustoUnitario", precoCustoUnitario);
            relatorio.put("ingredientes", ingredientes);
            relatorio.put("vendasPorFormaPagamento", vendasPorFormaPagamento);
            relatorio.put("taxaConversao", taxaConversao);
            relatorio.put("margemLucroBruto", margemLucroBruto);
            relatorio.put("kpiData", Map.of(
                    "taxaConversao", taxaConversao,
                    "margemLucroBruto", margemLucroBruto
            ));

        } catch (Exception e) {
            logger.error("Erro ao gerar relatório.", e);
            throw e;
        }

        return relatorio;
    }

    private List<Object[]> calcularVendasPorFormaPagamento(LocalDateTime start, LocalDateTime end, Vendido canal) {
        return (canal == Vendido.ALL)
                ? vendaRepository.findVendasAgrupadasPorFormaPagamento(start, end)
                : vendaRepository.findVendasAgrupadasPorFormaPagamentoAndChannel(start, end, canal);
    }
}
