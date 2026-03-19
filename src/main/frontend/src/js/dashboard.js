// Variáveis globais
let allIngredientes = [];
let ingredientesOriginais = [];
const escapeHtml = (value) => String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');

document.addEventListener('DOMContentLoaded', () => {
    console.log('Dashboard carregada! Iniciando...');

    // Configuração das URLs da API
    const API_URLS = Object.freeze({
        INGREDIENTES_LISTAR: '/ingredientes/listar',
        INGREDIENTES_ATUALIZAR_ESTOQUE: '/ingredientes',
        INGREDIENTES_ADICIONAR_LOTE: '/ingredientes',
        PRODUTOS_LISTAR: '/api/produtos',
        PRODUTOS_EDITAR: '/api/produtos',
        PRODUTOS_DELETAR: '/api/produtos',
        INGREDIENTES_SALVAR: '/ingredientes/salvar',
        INGREDIENTES_EDITAR: '/ingredientes/editar',
        INGREDIENTES_DELETAR: '/ingredientes/deletar'
    });

    // Tokens CSRF para segurança
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
    if (!csrfToken || !csrfHeader) console.warn('CSRF Token ou Header não encontrado.');

    // ==========================================================================
    // INICIALIZAÇÃO
    // ==========================================================================

    /**
     * Inicializa a dashboard carregando dados e configurando eventos
     */
    async function init() {
        console.log('Inicializando dashboard...');
        try {
            await carregarIngredientes();
            console.log('Ingredientes carregados com sucesso.');
            if (document.getElementById('tabelaIngredientes')) {
                renderizarTabelaIngredientes();
            }
            atualizarValorTotalIngredientes();
            await atualizarTabelaProdutos();
            configurarModalNovoIngrediente();
            configurarFormularioEdicaoProduto();
            configurarFormularioEdicaoIngrediente();
            configurarModalAdicionarLote();
        } catch (err) {
            console.error('Erro na inicialização do dashboard:', err);
            showErrorToast('Erro ao inicializar dashboard: ' + err.message);
        }
    }

    // ==========================================================================
    // FUNÇÕES DE INGREDIENTES
    // ==========================================================================

    /**
     * Carrega os ingredientes do backend
     * @returns {Promise<Array>} Lista de ingredientes
     */
    async function carregarIngredientes() {
        console.log('Carregando ingredientes do endpoint:', API_URLS.INGREDIENTES_LISTAR);
        try {
            const response = await fetch(API_URLS.INGREDIENTES_LISTAR, {
                headers: {
                    'Accept': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Erro ao carregar ingredientes: ${response.status} - ${errorText}`);
            }
            allIngredientes = await response.json();
            console.log('Ingredientes carregados:', allIngredientes);

            const select = document.getElementById('novoIngredienteNome');
            if (select) {
                select.innerHTML = '<option value="">Selecione ou crie um novo ingrediente</option><option value="novo">+ Novo Ingrediente</option>';
                allIngredientes.forEach(ing => {
                    const option = document.createElement('option');
                    option.value = ing.id;
                    option.textContent = ing.nome;
                    select.appendChild(option);
                });
            } else {
                console.warn('Select #novoIngredienteNome não encontrado.');
            }
            return allIngredientes;
        } catch (error) {
            console.error('Falha ao carregar ingredientes:', error);
            showErrorToast('Erro ao carregar ingredientes: ' + error.message);
            throw error;
        }
    }

    /**
     * Configura o comportamento do modal de novo ingrediente
     */
    function configurarModalNovoIngrediente() {
        console.log('Configurando modal de novo ingrediente...');
        const select = document.getElementById('novoIngredienteNome');
        const nomeTextoContainer = document.getElementById('novoIngredienteNomeTextoContainer');
        const unidadeSelect = document.getElementById('novoIngredienteUnidade');
        const form = document.getElementById('formNovoIngrediente');

        if (!select || !nomeTextoContainer || !unidadeSelect || !form) {
            console.error('Elementos do modal de novo ingrediente não encontrados:', { select, nomeTextoContainer, unidadeSelect, form });
            return;
        }

        select.addEventListener('change', () => {
            console.log('Seleção de ingrediente alterada para:', select.value);
            if (select.value === 'novo') {
                nomeTextoContainer.style.display = 'block';
                unidadeSelect.disabled = false;
                unidadeSelect.value = '';
            } else {
                nomeTextoContainer.style.display = 'none';
                unidadeSelect.disabled = true;
                const ingrediente = allIngredientes.find(ing => ing.id == select.value);
                unidadeSelect.value = ingrediente?.unidadeMedida || '';
            }
        });

        const modal = document.getElementById('novoIngredienteModal');
        if (modal) {
            modal.addEventListener('show.bs.modal', () => {
                console.log('Abrindo modal de novo ingrediente, resetando formulário...');
                form.reset();
                select.value = '';
                nomeTextoContainer.style.display = 'none';
                unidadeSelect.disabled = false;
                unidadeSelect.value = '';
            });
        }

        const salvarBtn = document.getElementById('salvarNovoIngredienteBtn');
        if (salvarBtn) {
            salvarBtn.addEventListener('click', salvarNovoIngrediente);
        } else {
            console.error('Botão #salvarNovoIngredienteBtn não encontrado.');
        }
    }

    /**
     * Salva um novo ingrediente ou adiciona um lote a um existente
     */
    async function salvarNovoIngrediente() {
        console.log('Iniciando salvamento de novo ingrediente ou lote...');
        const select = document.getElementById('novoIngredienteNome');
        const nomeTexto = document.getElementById('novoIngredienteNomeTexto')?.value.trim();
        const unidade = document.getElementById('novoIngredienteUnidade')?.value;
        const loteQuantidade = parseFloat(document.getElementById('novoLoteQuantidade')?.value || 'NaN');
        const precoTotalLote = parseFloat(document.getElementById('precoTotalLote')?.value || 'NaN');

        if (isNaN(loteQuantidade) || isNaN(precoTotalLote) || loteQuantidade <= 0 || precoTotalLote <= 0) { // Ajustado para <= 0
            console.warn('Valores inválidos detectados:', { loteQuantidade, precoTotalLote });
            showErrorToast('Quantidade e preço total devem ser maiores que zero!');
            return;
        }

        if (!select?.value) {
            console.warn('Nenhum ingrediente selecionado.');
            showErrorToast('Selecione um ingrediente ou crie um novo!');
            return;
        }

        const isNovoIngrediente = select.value === 'novo';
        const modal = document.getElementById('novoIngredienteModal');
        const modalInstance = bootstrap.Modal.getInstance(modal);

        try {
            if (isNovoIngrediente) {
                if (!nomeTexto || !unidade) {
                    console.warn('Faltam dados para novo ingrediente:', { nomeTexto, unidade });
                    showErrorToast('Preencha nome e unidade para novo ingrediente!');
                    return;
                }
                const custoPorUnidade = precoTotalLote / loteQuantidade; // Não precisa verificar > 0, já validado acima
                const formData = {
                    ingrediente: {
                        nome: nomeTexto,
                        unidadeMedida: unidade,
                        estoqueInicial: loteQuantidade,
                        estoqueAtual: 0
                    },
                    lote: {
                        quantidade: loteQuantidade,
                        custoPorUnidade: custoPorUnidade
                    }
                };

                console.log('Enviando novo ingrediente:', formData);
                const response = await fetch(API_URLS.INGREDIENTES_SALVAR, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrfHeader]: csrfToken
                    },
                    body: JSON.stringify(formData)
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(`Erro ${response.status}: ${errorData.error || 'Erro desconhecido'}`);
                }
                const data = await response.json();
                console.log('Novo ingrediente salvo:', data);
                showSuccessToast(`Novo ingrediente "${nomeTexto}" cadastrado!`);
            } else {
                // ... código existente para adicionar lote a ingrediente existente ...
            }

            await carregarIngredientes();
            renderizarTabelaIngredientes();
            atualizarValorTotalIngredientes();
            await atualizarTabelaProdutos();
        } catch (error) {
            console.error('Erro ao salvar ingrediente ou lote:', error);
            showErrorToast(`Erro ao salvar: ${error.message}`);
        } finally {
            if (modalInstance) {
                modalInstance.hide();
                console.log('Modal de novo ingrediente fechado.');
            }
            document.querySelector('.modal-backdrop')?.remove();
            modal?.classList.remove('show');
            document.body.classList.remove('modal-open');
            console.log('Estado do modal restaurado.');
        }
    }

    /**
     * Calcula e atualiza o valor total dos ingredientes em estoque
     */
    function atualizarValorTotalIngredientes() {
        console.log('Calculando valor total dos ingredientes...');
        const valorTotalElement = document.getElementById('valorTotalIngredientes');
        if (!valorTotalElement) {
            console.warn('Elemento #valorTotalIngredientes não encontrado.');
            return;
        }

        try {
            const valorTotal = allIngredientes.reduce((total, ing) => {
                const estoque = parseFloat(ing.estoqueAtual) || 0;
                const custo = parseFloat(ing.custoPorUnidade) || 0;
                return total + (estoque * custo);
            }, 0);
            valorTotalElement.textContent = valorTotal.toFixed(2).replace('.', ',');
            console.log('Valor total atualizado:', valorTotal);
        } catch (error) {
            console.error('Erro ao calcular valor total:', error);
            valorTotalElement.textContent = 'Erro';
            showErrorToast('Erro ao calcular valor total dos ingredientes.');
        }
    }

    /**
     * Renderiza a tabela de ingredientes
     */
    function renderizarTabelaIngredientes() {
        console.log('Renderizando tabela de ingredientes...');
        const tabela = document.getElementById('tabelaIngredientes');
        if (!tabela) {
            return;
        }

        const tbody = tabela.querySelector('tbody');
        tbody.innerHTML = '';
        if (!Array.isArray(allIngredientes) || allIngredientes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-muted text-center">Nenhum ingrediente cadastrado.</td></tr>';
            console.log('Nenhum ingrediente para renderizar.');
            return;
        }

        allIngredientes.forEach(ing => {
            const row = `
                <tr>
                    <td>${escapeHtml(ing.nome || 'N/A')}</td>
                    <td>${escapeHtml(ing.unidadeMedida || 'N/A')}</td>
                    <td>${ing.estoqueAtual || 0}</td>
                    <td>${(ing.custoPorUnidade || 0).toFixed(2)}</td>
                    <td>
                        <button class="btn btn-success btn-sm btn-adicionar-lote" data-id="${escapeHtml(ing.id)}">
                            <i class="bi bi-plus-circle"></i> <span class="d-none d-md-inline">Adicionar Lote</span>
                        </button>
                        <button class="btn btn-warning btn-sm btn-editar-ingrediente" data-id="${escapeHtml(ing.id)}">
                            <i class="bi bi-pencil"></i> <span class="d-none d-md-inline">Editar</span>
                        </button>
                        <button class="btn btn-danger btn-sm btn-deletar-ingrediente" data-id="${escapeHtml(ing.id)}">
                            <i class="bi bi-trash"></i> <span class="d-none d-md-inline">Deletar</span>
                        </button>
                    </td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', row);
        });

        tbody.removeEventListener('click', handleIngredienteActions);
        tbody.addEventListener('click', handleIngredienteActions);
        console.log('Tabela de ingredientes renderizada com sucesso.');
    }

    /**
     * Manipula ações na tabela de ingredientes
     * @param {Event} e - Evento de clique
     */
    function handleIngredienteActions(e) {
        const target = e.target.closest('button');
        if (!target) return;

        const id = target.getAttribute('data-id');
        if (target.classList.contains('btn-adicionar-lote')) {
            console.log('Iniciando ação de adicionar lote para ingrediente ID:', id);
            abrirModalAdicionarLote(id);
        } else if (target.classList.contains('btn-editar-ingrediente')) {
            console.log('Iniciando edição do ingrediente ID:', id);
            abrirModalEdicaoIngrediente(id);
        } else if (target.classList.contains('btn-deletar-ingrediente')) {
            console.log('Iniciando deleção do ingrediente ID:', id);
            deletarIngredienteHandler(id);
        }
    }

    /**
     * Abre o modal de edição de ingrediente
     * @param {string} id - ID do ingrediente
     */
    function abrirModalEdicaoIngrediente(id) {
        console.log('Abrindo modal para edição do ingrediente ID:', id);
        const ingrediente = allIngredientes.find(ing => ing.id == id);
        if (!ingrediente) {
            console.error('Ingrediente não encontrado:', id);
            showErrorToast('Ingrediente não encontrado!');
            return;
        }

        try {
            preencherFormularioEdicaoIngrediente(ingrediente);
            new bootstrap.Modal(document.getElementById('editarIngredienteModal')).show();
            console.log('Modal de edição aberto com sucesso.');
        } catch (error) {
            console.error('Erro ao abrir modal de edição:', error);
            showErrorToast('Erro ao abrir edição de ingrediente.');
        }
    }

    /**
     * Preenche o formulário de edição de ingrediente
     * @param {Object} data - Dados do ingrediente
     */
    function preencherFormularioEdicaoIngrediente(data) {
        console.log('Preenchendo formulário de edição do ingrediente:', data);
        const formulario = {
            id: document.getElementById('editarIngredienteId'),
            nome: document.getElementById('editarIngredienteNome'),
            unidade: document.getElementById('editarIngredienteUnidade'),
            estoque: document.getElementById('editarIngredienteEstoque'),
            custo: document.getElementById('editarIngredienteCusto')
        };

        if (Object.values(formulario).some(el => !el)) {
            console.error('Elementos do formulário de edição não encontrados:', formulario);
            throw new Error('Formulário de edição incompleto.');
        }

        formulario.id.value = data.id || '';
        formulario.nome.value = data.nome || '';
        formulario.unidade.value = data.unidadeMedida || '';
        formulario.estoque.value = data.estoqueAtual || 0;
        formulario.custo.value = parseFloat(data.custoPorUnidade) || 0;
        console.log('Formulário de edição preenchido com sucesso.');
    }

    /**
     * Envia os dados de edição de ingrediente
     */
    async function enviarFormularioEdicaoIngrediente() {
        console.log('Enviando formulário de edição de ingrediente...');
        const form = document.getElementById('formEditarIngrediente');
        if (!form) {
            console.error('Formulário #formEditarIngrediente não encontrado.');
            return;
        }

        const modal = document.getElementById('editarIngredienteModal');
        const modalInstance = bootstrap.Modal.getInstance(modal);

        try {
            const formData = new FormData(form);
            const data = {
                id: formData.get('id'),
                nome: formData.get('nome'),
                unidadeMedida: formData.get('unidadeMedida'),
                estoqueAtual: parseFloat(formData.get('estoqueAtual') || '0'),
                custoPorUnidade: parseFloat(formData.get('custoPorUnidade') || '0')
            };
            console.log('Dados enviados:', data);

            const response = await fetch(API_URLS.INGREDIENTES_EDITAR, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(data)
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Erro ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('Ingrediente editado com sucesso:', result);
            showSuccessToast(result.message || 'Ingrediente atualizado!');

            await carregarIngredientes();
            renderizarTabelaIngredientes();
            atualizarValorTotalIngredientes();
        } catch (error) {
            console.error('Erro ao editar ingrediente:', error);
            showErrorToast(`Erro ao editar ingrediente: ${error.message}`);
        } finally {
            if (modalInstance) {
                modalInstance.hide();
                console.log('Modal de edição de ingrediente fechado.');
            }
            document.querySelector('.modal-backdrop')?.remove();
            modal?.classList.remove('show');
            document.body.classList.remove('modal-open');
            console.log('Estado do modal restaurado.');
        }
    }

    /**
     * Handler para deleção de ingrediente
     * @param {string} id - ID do ingrediente
     */
    function deletarIngredienteHandler(id) {
        if (confirm('Tem certeza que deseja deletar este ingrediente?')) {
            console.log('Confirmação recebida, deletando ID:', id);
            deletarIngrediente(id);
        }
    }

    /**
     * Deleta um ingrediente
     * @param {string} id - ID do ingrediente
     */
    async function deletarIngrediente(id) {
        console.log('Deletando ingrediente ID:', id);
        try {
            const response = await fetch(`${API_URLS.INGREDIENTES_DELETAR}/${id}`, {
                method: 'DELETE',
                headers: { [csrfHeader]: csrfToken }
            });
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || `Erro ${response.status}`);
            }
            const data = await response.json();
            console.log('Ingrediente deletado:', data);
            showSuccessToast(data.message || 'Ingrediente deletado!');

            await carregarIngredientes();
            renderizarTabelaIngredientes();
            atualizarValorTotalIngredientes();
            await atualizarTabelaProdutos();
        } catch (error) {
            console.error('Erro ao deletar ingrediente:', error);
            showErrorToast(`Erro ao deletar: ${error.message}`);
        }
    }

    /**
     * Configura o formulário de edição de ingrediente
     */
    function configurarFormularioEdicaoIngrediente() {
        console.log('Configurando formulário de edição de ingrediente...');
        const form = document.getElementById('formEditarIngrediente');
        if (!form) {
            console.error('Formulário #formEditarIngrediente não encontrado.');
            return;
        }
        form.addEventListener('submit', e => {
            e.preventDefault();
            enviarFormularioEdicaoIngrediente();
        });
    }

    /**
     * Configura o modal de adicionar lote
     */
    function configurarModalAdicionarLote() {
        console.log('Configurando modal de adicionar lote...');
        const modal = document.getElementById('adicionarLoteModal');
        const form = document.getElementById('formAdicionarLote');
        const salvarBtn = document.getElementById('salvarLoteBtn');

        if (!modal || !form || !salvarBtn) {
            console.error('Elementos do modal de adicionar lote não encontrados:', { modal, form, salvarBtn });
            return;
        }

        salvarBtn.addEventListener('click', salvarLote);

        modal.addEventListener('show.bs.modal', () => {
            console.log('Abrindo modal de adicionar lote, resetando formulário...');
            form.reset();
        });

        modal.addEventListener('hidden.bs.modal', () => {
            console.log('Modal de adicionar lote fechado.');
            document.querySelector('.modal-backdrop')?.remove();
            document.body.classList.remove('modal-open');
        });
    }

    /**
     * Abre o modal de adicionar lote
     * @param {string} id - ID do ingrediente
     */
    function abrirModalAdicionarLote(id) {
        console.log('Abrindo modal para adicionar lote ao ingrediente ID:', id);
        const ingrediente = allIngredientes.find(ing => ing.id == id);
        if (!ingrediente) {
            console.error('Ingrediente não encontrado:', id);
            showErrorToast('Ingrediente não encontrado!');
            return;
        }

        try {
            const ingredienteIdInput = document.getElementById('loteIngredienteId');
            if (!ingredienteIdInput) {
                console.error('Input #loteIngredienteId não encontrado.');
                throw new Error('Formulário de lote incompleto.');
            }
            ingredienteIdInput.value = id;
            new bootstrap.Modal(document.getElementById('adicionarLoteModal')).show();
            console.log('Modal de adicionar lote aberto com sucesso.');
        } catch (error) {
            console.error('Erro ao abrir modal de adicionar lote:', error);
            showErrorToast('Erro ao abrir modal de lote.');
        }
    }

    /**
     * Salva o novo lote
     */
    async function salvarLote() {
        console.log('Iniciando salvamento de novo lote...');
        const form = document.getElementById('formAdicionarLote');
        if (!form) {
            console.error('Formulário #formAdicionarLote não encontrado.');
            return;
        }

        const id = document.getElementById('loteIngredienteId')?.value;
        const quantidade = parseFloat(document.getElementById('loteQuantidade')?.value || '0');
        const valorTotal = parseFloat(document.getElementById('loteValorTotal')?.value || '0');
        const modal = document.getElementById('adicionarLoteModal');
        const modalInstance = bootstrap.Modal.getInstance(modal);

        if (!id) {
            console.warn('ID do ingrediente não fornecido.');
            showErrorToast('Ingrediente não identificado!');
            return;
        }

        if (quantidade <= 0 || valorTotal < 0) {
            console.warn('Valores inválidos para lote:', { quantidade, valorTotal });
            showErrorToast('Quantidade deve ser > 0 e valor total >= 0!');
            return;
        }

        try {
            console.log('Enviando dados do lote para o backend:', { id, quantidade, valorTotal });
            const response = await fetch(`${API_URLS.INGREDIENTES_ADICIONAR_LOTE}/${id}/adicionar-lote?quantidade=${quantidade}&valorTotal=${valorTotal}`, {
                method: 'POST',
                headers: { [csrfHeader]: csrfToken }
            });
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(`Erro ${response.status}: ${errorData.error || 'Erro desconhecido'}`);
            }
            const data = await response.json();
            console.log('Lote adicionado com sucesso:', data);
            showSuccessToast(data.message || 'Lote adicionado com sucesso!');

            await carregarIngredientes();
            renderizarTabelaIngredientes();
            atualizarValorTotalIngredientes();
        } catch (error) {
            console.error('Erro ao adicionar lote:', error);
            showErrorToast(`Erro ao adicionar lote: ${error.message}`);
        } finally {
            if (modalInstance) {
                modalInstance.hide();
                console.log('Modal de adicionar lote fechado.');
            }
            document.querySelector('.modal-backdrop')?.remove();
            modal?.classList.remove('show');
            document.body.classList.remove('modal-open');
            console.log('Estado do modal restaurado.');
        }
    }

    // ==========================================================================
    // FUNÇÕES DE PRODUTOS
    // ==========================================================================

    /**
     * Renderiza a tabela de produtos
     * @param {Array} data - Lista de produtos
     */
    function renderizarTabelaProdutos(data) {
        console.log('Renderizando tabela de produtos...');
        const tabela = document.getElementById('tabelaProdutos');
        if (!tabela) {
            console.warn('Tabela #tabelaProdutos não encontrada.');
            return;
        }

        const tbody = tabela.querySelector('tbody');
        tbody.innerHTML = '';
        if (!Array.isArray(data) || data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-muted text-center">Nenhum produto cadastrado.</td></tr>';
            return;
        }

        data.forEach(produto => {
            const ingredientesHTML = Array.isArray(produto.ingredientes) && produto.ingredientes.length > 0
                ? produto.ingredientes.map(ing => `<span class="badge bg-light text-dark me-1 fw-bold">${escapeHtml(ing.nome || 'N/A')}</span>`).join('')
                : '<span class="text-muted">Nenhum ingrediente</span>';
            const estoqueHTML = produto.estoqueAtual < 10
                ? `<span class="text-danger fw-bold">${produto.estoqueAtual}</span>`
                : produto.estoqueAtual;

            const row = `
                <tr>
                    <td>${escapeHtml(produto.sabor || 'N/A')}</td>
                    <td class="mobile-hide">${ingredientesHTML}</td>
                    <td>${estoqueHTML}</td>
                    <td>${(produto.precoCustoUnitario || 0).toFixed(2)}</td>
                    <td>
                        <button class="btn btn-warning btn-sm btn-editar-produto" data-id="${escapeHtml(produto.id)}">
                            <i class="bi bi-pencil"></i> <span class="d-none d-md-inline">Editar</span>
                        </button>
                        <button class="btn btn-danger btn-sm btn-deletar-produto" data-id="${escapeHtml(produto.id)}">
                            <i class="bi bi-trash"></i> <span class="d-none d-md-inline">Deletar</span>
                        </button>
                    </td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', row);
        });

        configurarBotoesEditarProdutos();
        configurarBotoesDeletarProdutos();
        console.log('Tabela de produtos renderizada com sucesso.');
    }

    /**
     * Atualiza a tabela de produtos
     */
    async function atualizarTabelaProdutos() {
        console.log('Atualizando tabela de produtos...');
        try {
            const response = await fetch(`${API_URLS.PRODUTOS_LISTAR}?timestamp=${new Date().getTime()}`, {
                headers: {
                    'Accept': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Erro ao listar produtos: ${response.status} - ${errorText}`);
            }
            const data = await response.json();
            console.log('Produtos carregados:', data);
            renderizarTabelaProdutos(data);
        } catch (error) {
            console.error('Erro ao atualizar produtos:', error);
            const tbody = document.querySelector('#tabelaProdutos tbody');
            if (tbody) {
                tbody.innerHTML = `<tr><td colspan="5" class="text-danger">Erro ao carregar: ${escapeHtml(error.message)}</td></tr>`;
            }
            showErrorToast('Erro ao carregar produtos: ' + error.message);
        }
    }

    /**
     * Configura os botões de edição de produtos
     */
    function configurarBotoesEditarProdutos() {
        console.log('Configurando botões de edição de produtos...');
        document.querySelectorAll('.btn-editar-produto').forEach(botao => {
            botao.onclick = () => abrirModalEdicaoProduto(botao);
        });
    }

    /**
     * Configura os botões de deleção de produtos
     */
    function configurarBotoesDeletarProdutos() {
        console.log('Configurando botões de deleção de produtos...');
        document.querySelectorAll('.btn-deletar-produto').forEach(botao => {
            botao.onclick = () => deletarProdutoHandler(botao);
        });
    }

    /**
     * Abre o modal de edição de produto
     * @param {HTMLElement} botao - Botão clicado
     */
    async function abrirModalEdicaoProduto(botao) {
        const id = botao.getAttribute('data-id');
        console.log('Abrindo modal de edição do produto ID:', id);
        if (!id) {
            console.error('ID não encontrado no botão:', botao);
            showErrorToast('ID do produto não encontrado.');
            return;
        }

        try {
            const response = await fetch(`${API_URLS.PRODUTOS_LISTAR}/${id}`, {
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Erro ao carregar produto: ${response.status}`);
            const data = await response.json();
            console.log('Dados do produto:', data);
            ingredientesOriginais = [...data.ingredientes];
            preencherFormularioEdicaoProduto(data);
            new bootstrap.Modal(document.getElementById('editarProdutoModal')).show();
            console.log('Modal de edição de produto aberto com sucesso.');
        } catch (error) {
            console.error('Erro ao carregar produto:', error);
            showErrorToast(`Erro ao carregar produto: ${error.message}`);
        }
    }

    /**
     * Preenche o formulário de edição de produto
     * @param {Object} data - Dados do produto
     */
    function preencherFormularioEdicaoProduto(data) {
        console.log('Preenchendo formulário de edição do produto:', data);
        const formulario = {
            id: document.getElementById('editarProdutoId'),
            categoria: document.getElementById('editarProdutoCategoria'),
            categoriaLabel: document.getElementById('editarProdutoCategoriaLabel'),
            sabor: document.getElementById('editarProdutoSabor'),
            estoqueInicial: document.getElementById('editarProdutoEstoqueInicial'),
            estoqueAtual: document.getElementById('editarProdutoEstoqueAtual'),
            precoCusto: document.getElementById('editarProdutoPrecoCusto'),
            precoCustoUnitario: document.getElementById('editarProdutoPrecoCustoUnitario')
        };

        if (Object.values(formulario).some(el => !el)) {
            console.error('Elementos do formulário de edição não encontrados:', formulario);
            throw new Error('Formulário de edição incompleto.');
        }

        formulario.id.value = data.id || '';
        formulario.categoria.value = data.categoria || '';
        formulario.categoriaLabel.value = (data.categoria || '')
            .replaceAll('_', ' ')
            .toLowerCase()
            .replace(/\b\w/g, letter => letter.toUpperCase());
        formulario.sabor.value = data.sabor || '';
        formulario.estoqueInicial.value = data.estoqueInicial || 0;
        formulario.estoqueAtual.value = data.estoqueAtual || 0;
        formulario.precoCusto.value = parseFloat(data.precoCusto) || 0;
        formulario.precoCustoUnitario.value = parseFloat(data.precoCustoUnitario) || 0;

        const container = document.getElementById('editarIngredientesContainer');
        if (container) {
            container.innerHTML = '';
            if (Array.isArray(data.ingredientes)) {
                data.ingredientes.forEach((ing, index) => {
                    const row = criarLinhaIngrediente(index, ing);
                    container.insertAdjacentHTML('beforeend', row);
                    configurarEventosLinhaIngrediente(container.children[index], ing);
                });
                calcularPrecoCustoProduto();
            }
        }
        console.log('Formulário de edição preenchido com sucesso.');
    }

    /**
     * Adiciona uma nova linha de ingrediente ao formulário
     */
    function addIngredienteProduto() {
        console.log('Adicionando nova linha de ingrediente ao produto...');
        const container = document.getElementById('editarIngredientesContainer');
        if (!container) {
            console.warn('Container #editarIngredientesContainer não encontrado.');
            return;
        }

        const index = container.children.length;
        const row = criarLinhaIngrediente(index, {});
        container.insertAdjacentHTML('beforeend', row);
        configurarEventosLinhaIngrediente(container.children[index], {});
        calcularPrecoCustoProduto();
    }

    /**
     * Cria uma linha de ingrediente
     * @param {number} index - Índice da linha
     * @param {Object} ingrediente - Dados do ingrediente
     * @returns {string} HTML da linha
     */
    function criarLinhaIngrediente(index, ingrediente) {
        const options = allIngredientes.map(ing =>
            `<option value="${escapeHtml(ing.id)}">${escapeHtml(ing.nome)} - R$${ing.custoPorUnidade.toFixed(2)} (${escapeHtml(ing.unidadeMedida)}, ${ing.estoqueAtual})</option>`
        ).join('');
        const isExistente = ingrediente.ingredienteId && ingrediente.nome;

        return `
            <div class="ingrediente-item row g-2 mb-2" data-index="${index}">
                <div class="col-5">
                    ${isExistente ?
                        `<input type="text" class="form-control" value="${escapeHtml(ingrediente.nome)}" data-ingrediente-id="${escapeHtml(ingrediente.ingredienteId)}" readonly>` :
                        `<select class="form-select" name="ingredientes[${index}].ingredienteId" required>
                            <option value="">Selecione um ingrediente</option>
                            ${options}
                        </select>`
                    }
                </div>
                <div class="col-5">
                    <input class="form-control" type="number" name="ingredientes[${index}].quantidade"
                           placeholder="Quantidade" min="0" step="0.001" value="${escapeHtml(ingrediente.quantidade || '')}" required>
                </div>
                <div class="col-2">
                    <i class="bi bi-x-circle-fill text-danger" style="cursor: pointer;"></i>
                </div>
            </div>
        `;
    }

    /**
     * Configura eventos para uma linha de ingrediente
     * @param {HTMLElement} row - Linha do ingrediente
     * @param {Object} ingredienteOriginal - Dados originais do ingrediente
     */
    function configurarEventosLinhaIngrediente(row, ingredienteOriginal) {
        const select = row.querySelector('select[name$=".ingredienteId"]');
        const quantidadeInput = row.querySelector('input[name$=".quantidade"]');
        const removeBtn = row.querySelector('.bi-x-circle-fill');

        if (select) {
            select.addEventListener('change', () => atualizarEstoqueIngrediente(row, ingredienteOriginal));
        }
        if (quantidadeInput) {
            quantidadeInput.addEventListener('input', () => atualizarEstoqueIngrediente(row, ingredienteOriginal));
        }
        if (removeBtn) {
            removeBtn.addEventListener('click', () => removerIngrediente(removeBtn, ingredienteOriginal));
        }
    }

    /**
     * Remove uma linha de ingrediente e reverte o estoque
     * @param {HTMLElement} element - Elemento clicado
     * @param {Object} ingredienteOriginal - Dados originais do ingrediente
     */
    async function removerIngrediente(element, ingredienteOriginal) {
        console.log('Removendo ingrediente do formulário...');
        const item = element.closest('.ingrediente-item');
        if (!item) {
            console.warn('Item de ingrediente não encontrado para remoção.');
            return;
        }

        const id = ingredienteOriginal.ingredienteId;
        const quantidadeOriginal = parseFloat(ingredienteOriginal.quantidade || '0');
        if (id && quantidadeOriginal > 0) {
            console.log('Revertendo estoque do ingrediente ID:', id, 'Quantidade:', quantidadeOriginal);
            try {
                await atualizarEstoqueBackend(id, quantidadeOriginal);
            } catch (error) {
                console.error('Erro ao reverter estoque:', error);
                showErrorToast(`Erro ao reverter estoque: ${error.message}`);
                return;
            }
        }

        item.remove();
        const container = document.getElementById('editarIngredientesContainer');
        Array.from(container.children).forEach((child, index) => {
            child.setAttribute('data-index', index);
            const inputs = child.querySelectorAll('input, select');
            inputs.forEach(input => {
                const name = input.getAttribute('name');
                if (name) input.setAttribute('name', name.replace(/\[\d+\]/, `[${index}]`));
            });
        });

        calcularPrecoCustoProduto();
        await carregarIngredientes();
        renderizarTabelaIngredientes();
        atualizarValorTotalIngredientes();
        console.log('Ingrediente removido e estoque atualizado.');
    }

    /**
     * Atualiza o estoque de um ingrediente no backend
     * @param {HTMLElement} row - Linha do ingrediente
     * @param {Object} ingredienteOriginal - Dados originais do ingrediente
     */
    async function atualizarEstoqueIngrediente(row, ingredienteOriginal) {
        console.log('Atualizando estoque do ingrediente...');
        const select = row.querySelector('select[name$=".ingredienteId"]');
        const inputReadonly = row.querySelector('input[readonly]');
        const quantidadeInput = row.querySelector('input[name$=".quantidade"]');

        const id = select ? select.value : (inputReadonly ? inputReadonly.dataset.ingredienteId : null);
        const quantidadeNova = parseFloat(quantidadeInput?.value || '0');
        const quantidadeOriginal = parseFloat(ingredienteOriginal.quantidade || '0');

        if (!id || quantidadeNova < 0) {
            console.warn('ID ou quantidade inválida:', { id, quantidadeNova });
            showErrorToast('Selecione um ingrediente e insira uma quantidade válida!');
            return;
        }

        try {
            const ingrediente = allIngredientes.find(ing => ing.id == id);
            if (!ingrediente) {
                console.error('Ingrediente não encontrado:', id);
                showErrorToast('Ingrediente não encontrado!');
                return;
            }

            const diferenca = (quantidadeNova - quantidadeOriginal).toFixed(3);
            if (diferenca != 0) { // Use != para comparar string com número
                console.log('Atualizando estoque do ingrediente ID:', id, 'Diferença:', diferenca);
                await atualizarEstoqueBackend(id, -diferenca);
                ingredienteOriginal.quantidade = quantidadeNova;
                renderizarTabelaIngredientes();
                atualizarValorTotalIngredientes();
            }
            calcularPrecoCustoProduto();
        } catch (error) {
            console.error('Erro ao atualizar estoque:', error);
            showErrorToast(`Erro ao atualizar estoque: ${error.message}`);
            if (quantidadeInput) quantidadeInput.value = quantidadeOriginal;
        }
    }

    /**
     * Chama o backend para atualizar o estoque
     * @param {string} id - ID do ingrediente
     * @param {number} quantidade - Quantidade a adicionar (positiva) ou subtrair (negativa)
     */
    async function atualizarEstoqueBackend(id, quantidade) {
        console.log('Enviando atualização de estoque para o backend:', { id, quantidade });
        try {
            const response = await fetch(`${API_URLS.INGREDIENTES_ATUALIZAR_ESTOQUE}/${id}/atualizar-estoque?quantidade=${quantidade}`, {
                method: 'PATCH',
                headers: { [csrfHeader]: csrfToken }
            });
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(`Erro ${response.status}: ${errorData.error || 'Erro desconhecido'}`);
            }
            const data = await response.json();
            console.log('Estoque atualizado:', data);
            const ingrediente = allIngredientes.find(ing => ing.id == id);
            if (ingrediente) {
                ingrediente.estoqueAtual = data.estoqueAtual;
            }
            return data;
        } catch (error) {
            console.error('Erro ao atualizar estoque no backend:', error);
            throw error;
        }
    }

    /**
     * Calcula o custo total e unitário do produto
     */
    function calcularPrecoCustoProduto() {
        console.log('Calculando custo do produto...');
        const ingredientes = document.querySelectorAll('#editarIngredientesContainer .ingrediente-item');
        let custoTotal = 0;

        ingredientes.forEach(ingrediente => {
            const select = ingrediente.querySelector('select[name$=".ingredienteId"]');
            const inputReadonly = ingrediente.querySelector('input[readonly]');
            const quantidadeInput = ingrediente.querySelector('input[name$=".quantidade"]');

            const id = select ? select.value : (inputReadonly ? inputReadonly.dataset.ingredienteId : '');
            const quantidade = parseFloat(quantidadeInput?.value || '0');

            if (id && quantidade > 0) {
                const ing = allIngredientes.find(i => i.id == id);
                if (ing) custoTotal += (parseFloat(ing.custoPorUnidade) || 0) * quantidade;
            }
        });

        const precoCustoInput = document.getElementById('editarProdutoPrecoCusto');
        const estoqueInicialInput = document.getElementById('editarProdutoEstoqueInicial');
        const precoUnitarioInput = document.getElementById('editarProdutoPrecoCustoUnitario');

        if (precoCustoInput && estoqueInicialInput && precoUnitarioInput) {
            const estoqueInicial = parseFloat(estoqueInicialInput.value) || 0;
            const precoUnitario = estoqueInicial > 0 ? custoTotal / estoqueInicial : 0;
            precoCustoInput.value = custoTotal.toFixed(2);
            precoUnitarioInput.value = precoUnitario.toFixed(2);
            console.log('Custo calculado:', { custoTotal, precoUnitario });
        } else {
            console.warn('Campos de custo não encontrados.');
        }
    }

    /**
     * Envia os dados de edição de produto
     */
    async function enviarFormularioEdicaoProduto() {
        console.log('Enviando formulário de edição de produto...');
        const form = document.getElementById('formEditarProduto');
        if (!form) {
            console.error('Formulário #formEditarProduto não encontrado.');
            return;
        }

        const modal = document.getElementById('editarProdutoModal');
        const modalInstance = bootstrap.Modal.getInstance(modal);

        try {
            const estoqueInicial = parseFloat(document.getElementById('editarProdutoEstoqueInicial').value || '0');
            if (estoqueInicial <= 0) {
                console.warn('Estoque inicial inválido:', estoqueInicial);
                showErrorToast('Estoque inicial deve ser maior que zero!');
                return;
            }

            const formData = new FormData(form);
            const ingredientes = Array.from(document.querySelectorAll('#editarIngredientesContainer .ingrediente-item'))
                .map(item => {
                    const select = item.querySelector('select[name$=".ingredienteId"]');
                    const inputReadonly = item.querySelector('input[readonly]');
                    const quantidadeInput = item.querySelector('input[name$=".quantidade"]');
                    const id = select ? select.value : (inputReadonly ? inputReadonly.dataset.ingredienteId : null);
                    const quantidade = parseFloat(quantidadeInput?.value || 0);
                    return id && quantidade > 0 ? { ingredienteId: id, quantidade } : null;
                })
                .filter(Boolean);

            const data = {
                id: formData.get('id'),
                sabor: formData.get('sabor'),
                estoqueInicial: parseInt(formData.get('estoqueInicial') || '0', 10),
                estoqueAtual: parseInt(formData.get('estoqueAtual') || '0', 10),
                precoCusto: parseFloat(formData.get('precoCusto') || '0'),
                precoCustoUnitario: parseFloat(formData.get('precoCustoUnitario') || '0'),
                categoria: formData.get('categoria'),
                ingredientes
            };
            console.log('Dados enviados:', data);

            const response = await fetch(`${API_URLS.PRODUTOS_EDITAR}/${data.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(data)
            });
            if (!response.ok) throw new Error(`Erro ao atualizar produto: ${response.status}`);
            const result = await response.json();
            console.log('Produto editado:', result);
            showSuccessToast(result.message || 'Produto atualizado!');

            await carregarIngredientes();
            await atualizarTabelaProdutos();
        } catch (error) {
            console.error('Erro ao editar produto:', error);
            showErrorToast(`Erro ao atualizar produto: ${error.message}`);
        } finally {
            if (modalInstance) {
                modalInstance.hide();
                console.log('Modal de edição de produto fechado.');
            }
            document.querySelector('.modal-backdrop')?.remove();
            modal?.classList.remove('show');
            document.body.classList.remove('modal-open');
            console.log('Estado do modal restaurado.');
        }
    }

    /**
     * Handler para deleção de produto
     * @param {HTMLElement} botao - Botão clicado
     */
    function deletarProdutoHandler(botao) {
        const id = botao.getAttribute('data-id');
        if (confirm('Tem certeza que deseja deletar este produto?')) {
            console.log('Confirmação recebida, deletando produto ID:', id);
            deletarProduto(id);
        }
    }

    /**
     * Deleta um produto
     * @param {string} id - ID do produto
     */
    async function deletarProduto(id) {
        console.log('Deletando produto ID:', id);
        try {
            const response = await fetch(`${API_URLS.PRODUTOS_DELETAR}/${id}`, {
                method: 'DELETE',
                headers: { [csrfHeader]: csrfToken }
            });
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || `Erro ${response.status}`);
            }
            const data = await response.json();
            console.log('Produto deletado:', data);
            showSuccessToast(data.message || 'Produto deletado!');
            await atualizarTabelaProdutos();
        } catch (error) {
            console.error('Erro ao deletar produto:', error);
            showErrorToast(`Erro ao deletar: ${error.message}`);
        }
    }

    /**
     * Configura o formulário de edição de produto
     */
    function configurarFormularioEdicaoProduto() {
        console.log('Configurando formulário de edição de produto...');
        const form = document.getElementById('formEditarProduto');
        if (form) {
            form.addEventListener('submit', e => {
                e.preventDefault();
                enviarFormularioEdicaoProduto();
            });
        } else {
            console.error('Formulário #formEditarProduto não encontrado.');
        }

        const addBtn = document.getElementById('addIngredienteBtnEditar');
        if (addBtn) addBtn.addEventListener('click', addIngredienteProduto);

        const estoqueInicialInput = document.getElementById('editarProdutoEstoqueInicial');
        if (estoqueInicialInput) estoqueInicialInput.addEventListener('input', calcularPrecoCustoProduto);

        const modal = document.getElementById('editarProdutoModal');
        if (modal) {
            modal.addEventListener('hidden.bs.modal', () => {
                console.log('Modal de edição de produto fechado');
                document.querySelector('.modal-backdrop')?.remove();
                document.body.classList.remove('modal-open');
                renderizarTabelaIngredientes();
                atualizarValorTotalIngredientes();
            });
        }
    }

    // ==========================================================================
    // FUNÇÕES AUXILIARES
    // ==========================================================================

    /**
     * Exibe um toast de sucesso
     * @param {string} message - Mensagem a ser exibida
     */
    function showSuccessToast(message) {
        console.log('Exibindo toast de sucesso:', message);
        const toastElement = document.getElementById('successToast');
        if (toastElement) {
            toastElement.querySelector('.toast-body').textContent = message;
            new bootstrap.Toast(toastElement).show();
        } else {
            console.warn('Toast de sucesso não encontrado, usando alert.');
            alert(message);
        }
    }

    /**
     * Exibe um toast de erro
     * @param {string} message - Mensagem a ser exibida
     */
    function showErrorToast(message) {
        console.error('Exibindo toast de erro:', message);
        const toastElement = document.getElementById('errorToast');
        if (toastElement) {
            toastElement.querySelector('.toast-body').textContent = message;
            new bootstrap.Toast(toastElement).show();
        } else {
            alert(message);
        }
    }

    // Inicia a dashboard
    init();
});
