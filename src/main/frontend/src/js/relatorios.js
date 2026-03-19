// Declaração dos gráficos como variáveis globais
let vendasChart, produtosChart, estoqueChart, vendasPorCanalChart, custoUnitarioChart, ingredientesChart, kpiChart, vendasPorFormaPagamentoChart;

// Paleta de 30 cores distintas para gráficos de pizza
const pieColors = [
    '#9370DB', '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0',
    '#9966FF', '#FF9F40', '#E7E9ED', '#C9CB3F', '#F7464A',
    '#66BB6A', '#AB47BC', '#42A5F5', '#FFA726', '#26C6DA',
    '#7E57C2', '#EF5350', '#29B6F6', '#D4E157', '#FF7043',
    '#8D6E63', '#BDBDBD', '#5C6BC0', '#F06292', '#26A69A',
    '#EC407A', '#9CCC65', '#FFCA28', '#78909C', '#D81B60'
];

// Configuração básica para todos os gráficos
const defaultChartConfig = {
    responsive: true,
    maintainAspectRatio: false
};

// Inicialização da aplicação ao carregar o DOM
document.addEventListener('DOMContentLoaded', () => {
    console.log('Inicializando aplicação...');
    initializeCharts();
    loadInitialData().catch(error => console.error('Erro ao carregar dados iniciais:', error));

    const applyFiltersButton = document.getElementById('applyFiltersButton');
    if (applyFiltersButton) {
        applyFiltersButton.addEventListener('click', () => applyFilters().catch(error => console.error('Erro ao aplicar filtros:', error)));
    } else {
        console.error('Botão "Aplicar Filtros" não encontrado no DOM.');
    }
});

// Função para inicializar todos os gráficos
function initializeCharts() {
    console.log('Inicializando gráficos...');

    vendasChart = createChart('vendasChart', 'line', 'Vendas por Período');
    produtosChart = createChart('produtosMaisVendidosChart', 'pie', 'Unidades Vendidas por Produto', { legend: 'right' });
    estoqueChart = createChart('estoqueChart', 'pie', 'Quantidade em Estoque por Produto', { legend: 'right' });
    vendasPorCanalChart = createChart('vendasPorCanalChart', 'bar', 'Vendas por Canal de Venda');
    custoUnitarioChart = createChart('custoUnitarioChart', 'bar', 'Preço de Custo Unitário por Produto');
    ingredientesChart = createChart('ingredientesChart', 'bar', 'Ingredientes: Preço e Estoque');
    kpiChart = createChart('kpiChart', 'bar', 'Indicadores de Performance (KPIs)');
    vendasPorFormaPagamentoChart = createChart('vendasPorFormaPagamentoChart', 'bar', 'Vendas por Forma de Pagamento');
}

// Função auxiliar para criar gráficos com configurações padrão
function createChart(elementId, type, title, additionalOptions = {}) {
    const ctx = document.getElementById(elementId);
    if (!ctx) {
        console.error(`Elemento com ID "${elementId}" não encontrado.`);
        return null;
    }

    return new Chart(ctx, {
        type,
        data: { datasets: [], labels: [] },
        options: {
            ...defaultChartConfig,
            plugins: {
                title: { text: title, display: true },
                legend: additionalOptions.legend ? { position: additionalOptions.legend } : undefined
            }
        }
    });
}

// Carrega dados iniciais (último mês)
async function loadInitialData() {
    const endDate = new Date().toISOString().split('T')[0];
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    const startDateStr = startDate.toISOString().split('T')[0];

    console.log(`Carregando dados iniciais de ${startDateStr} até ${endDate}...`);
    await applyFilters(startDateStr, endDate, 'ALL');
}

// Aplica filtros e atualiza os gráficos
async function applyFilters(customStart, customEnd, customChannel) {
    const start = customStart || document.getElementById('startDate')?.value;
    const end = customEnd || document.getElementById('endDate')?.value;
    const channel = customChannel || document.getElementById('salesChannel')?.value;

    try {
        validateDates(start, end);
        showLoadingSpinner(true);

        const url = `/relatorios/dados?startDate=${start}&endDate=${end}&salesChannel=${channel}`;
        console.log(`Buscando dados da API: ${url}`);

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Erro na requisição à API. Status: ${response.status}`);
        }

        const data = await response.json();
        console.log('Dados recebidos da API:', data);

        updateMetrics(data);
        updateCharts(data);
    } catch (error) {
        console.error('Erro ao aplicar filtros:', error);
        alert(`Erro ao carregar dados: ${error.message}`);
    } finally {
        showLoadingSpinner(false);
    }
}

// Valida as datas fornecidas
function validateDates(start, end) {
    const startDate = new Date(start);
    const endDate = new Date(end);

    if (isNaN(startDate.getTime())) throw new Error('Data inicial inválida');
    if (isNaN(endDate.getTime())) throw new Error('Data final inválida');
    if (startDate > endDate) throw new Error('A data inicial não pode ser maior que a data final');
}

// Atualiza as métricas exibidas na interface
function updateMetrics(data) {
    try {
        const totalVendas = data.totalVendas ?? 0;
        const ticketMedio = data.ticketMedio ?? 0;
        const clientesUnicos = data.clientesUnicos ?? 0;
        const taxaConversao = data.taxaConversao ?? 0;
        const margemLucroBruto = data.margemLucroBruto ?? 0;
        const produtosSemEstoque = Array.isArray(data.quantidadeEstoquePorProduto)
            ? data.quantidadeEstoquePorProduto.filter(item => (item[1] || 0) <= 0).length
            : 0;

        setElementText('totalVendas', totalVendas.toFixed(2));
        setElementText('ticketMedio', ticketMedio.toFixed(2));
        setElementText('clientesUnicos', clientesUnicos);
        setElementText('taxaConversao', taxaConversao.toFixed(1));
        setElementText('margemLucroBruto', margemLucroBruto.toFixed(1));
        setElementText('produtosFalta', produtosSemEstoque);

        console.log('Métricas atualizadas com sucesso.');
    } catch (error) {
        console.error('Erro ao atualizar métricas:', error);
    }
}

// Atualiza todos os gráficos com os dados recebidos
function updateCharts(data) {
    console.log('Atualizando gráficos...');
    clearCharts();

    updateChart(vendasChart, data.vendasPorPeriodo, {
        label: 'Vendas (R$)',
        dataKey: 1,
        borderColor: '#9370DB'
    });

    updatePieChart(produtosChart, data.produtosMaisVendidos, 'Unidades Vendidas');
    updatePieChart(estoqueChart, data.quantidadeEstoquePorProduto, 'Quantidade em Estoque');

    updateChart(vendasPorCanalChart, data.vendasPorCanal, {
        label: 'Vendas (R$)',
        dataKey: 1,
        backgroundColor: '#9370DB'
    });

    updateChart(custoUnitarioChart, data.precoCustoUnitario, {
        label: 'Preço de Custo Unitário (R$)',
        dataKey: 1,
        backgroundColor: '#9370DB'
    });

    updateIngredientesChart(data.ingredientes);
    updateKpiChart(data.kpiData);
    updateChart(vendasPorFormaPagamentoChart, data.vendasPorFormaPagamento, {
        label: 'Vendas (R$)',
        dataKey: 1,
        backgroundColor: '#9370DB'
    });
}

// Atualiza gráficos de linha ou barras
function updateChart(chart, data, { label, dataKey, borderColor, backgroundColor }) {
    if (!chart) return;
    if (Array.isArray(data) && data.length > 0) {
        chart.data.labels = data.map(item => item[0] || 'Desconhecido');
        chart.data.datasets = [{
            label,
            data: data.map(item => item[dataKey] || 0),
            borderColor: borderColor || undefined,
            backgroundColor: backgroundColor || undefined
        }];
        chart.update();
        console.log(`Gráfico ${chart.options.plugins.title.text} atualizado.`);
    } else {
        setEmptyChart(chart, label);
    }
}

// Atualiza gráficos de pizza
function updatePieChart(chart, data, label) {
    if (!chart) return;
    if (Array.isArray(data) && data.length > 0) {
        chart.data.labels = data.map(item => item[0] || 'Desconhecido');
        chart.data.datasets = [{
            label,
            data: data.map(item => item[1] || 0),
            backgroundColor: pieColors.slice(0, data.length),
            borderWidth: 1
        }];
        chart.update();
        console.log(`Gráfico ${chart.options.plugins.title.text} atualizado.`);
    } else {
        console.warn(`Dados inválidos ou vazios para ${chart.options.plugins.title.text}:`, data);
        setEmptyChart(chart, label);
    }
}

// Atualiza gráfico de ingredientes (barras múltiplas)
function updateIngredientesChart(data) {
    if (!ingredientesChart || !Array.isArray(data) || data.length === 0) {
        setEmptyChart(ingredientesChart, 'Preço (R$)');
        return;
    }

    ingredientesChart.data.labels = data.map(item => item.nome || 'Desconhecido');
    ingredientesChart.data.datasets = [
        {
            label: 'Preço (R$)',
            data: data.map(item => item.preco || 0),
            backgroundColor: '#9370DB'
        },
        {
            label: 'Estoque',
            data: data.map(item => item.estoque || 0),
            backgroundColor: '#FF6384'
        }
    ];
    ingredientesChart.update();
    console.log('Gráfico de ingredientes atualizado.');
}

// Atualiza gráfico de KPIs
function updateKpiChart(kpiData) {
    if (!kpiChart || !kpiData) {
        setEmptyChart(kpiChart, 'KPIs');
        return;
    }

    kpiChart.data.labels = ['Taxa de Conversão', 'Margem de Lucro Bruto'];
    kpiChart.data.datasets = [{
        label: 'KPIs',
        data: [kpiData.taxaConversao || 0, kpiData.margemLucroBruto || 0],
        backgroundColor: ['#9370DB', '#FF6384']
    }];
    kpiChart.update();
    console.log('Gráfico de KPIs atualizado.');
}

// Define um gráfico como vazio
function setEmptyChart(chart, label) {
    if (!chart) return;
    chart.data.labels = ['Nenhum dado disponível'];
    chart.data.datasets = [{
        label,
        data: [1],
        backgroundColor: ['#E7E9ED']
    }];
    chart.update();
    console.warn(`Gráfico ${chart.options.plugins.title.text} definido como vazio.`);
}

// Limpa todos os gráficos
function clearCharts() {
    [vendasChart, produtosChart, estoqueChart, vendasPorCanalChart, custoUnitarioChart,
     ingredientesChart, kpiChart, vendasPorFormaPagamentoChart].forEach(chart => {
        if (chart) {
            chart.data.labels = [];
            chart.data.datasets = [];
            chart.update();
        }
    });
    console.log('Todos os gráficos foram limpos.');
}

// Define o texto de um elemento, com fallback
function setElementText(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = value;
    } else {
        console.warn(`Elemento com ID "${elementId}" não encontrado.`);
    }
}

// Controla a exibição do spinner de carregamento
function showLoadingSpinner(show) {
    const spinner = document.getElementById('loadingSpinner');
    if (spinner) {
        spinner.style.display = show ? 'block' : 'none';
    } else {
        console.warn('Spinner de carregamento não encontrado.');
    }
}