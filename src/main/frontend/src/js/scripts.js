let produtoIdEditar = 0; // Variável global para armazenar o ID do produto sendo editado (não utilizada no momento)

document.addEventListener('DOMContentLoaded', function () {
    console.log('Dashboard carregada!');

    function salvarNovoIngrediente() {
        const formData = {
            nome: document.getElementById('novoIngredienteNome').value.trim(),
            custoPorUnidade: parseFloat(document.getElementById('novoIngredienteCusto').value),
            unidadeMedida: document.getElementById('novoIngredienteUnidade').value,
            estoqueInicial: parseFloat(document.getElementById('novoIngredienteEstoque').value)
        };

        if (!formData.nome || isNaN(formData.custoPorUnidade) || !formData.unidadeMedida || isNaN(formData.estoqueInicial)) {
            alert('Por favor, preencha todos os campos corretamente!');
            return;
        }

        console.log('Enviando dados para salvar ingrediente:', formData);

        fetch('/ingredientes/salvar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(formData)
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(`Erro ${response.status}: ${text}`); });
            }
            console.log('Resposta recebida do servidor para novo ingrediente');
            return response.json();
        })
        .then(data => {
            console.log('Dados retornados após salvar ingrediente:', data);
            const toastElement = document.getElementById('successToast');
            if (!toastElement) {
                console.error('Elemento successToast não encontrado!');
                alert('Novo ingrediente cadastrado com sucesso!'); // Fallback para alert
                return;
            }
            const toastBody = toastElement.querySelector('.toast-body');
            if (!toastBody) {
                console.error('Elemento toast-body não encontrado!');
                alert('Novo ingrediente cadastrado com sucesso!'); // Fallback para alert
                return;
            }
            toastBody.textContent = 'Novo ingrediente cadastrado com sucesso!';
            const toast = new bootstrap.Toast(toastElement);
            console.log('Exibindo toast para novo ingrediente');
            toast.show();
            carregarIngredientes(); // Atualiza a lista de ingredientes
            const novoIngredienteModal = bootstrap.Modal.getInstance(document.getElementById('novoIngredienteModal'));
            novoIngredienteModal.hide(); // Fecha o modal de novo ingrediente
            const editarModal = new bootstrap.Modal(document.getElementById('editarGeladinhoModal'));
            editarModal.show(); // Reabre o modal de edição
        })
        .catch(error => {
            alert('Erro ao salvar ingrediente: ' + error.message);
            console.error('Detalhes do erro ao salvar ingrediente:', error);
        });
    }

    // URLs das APIs
    const INGREDIENTES_API_URL = '/ingredientes/listar';
    const GELADINHOS_API_URL = '/geladinhos/listar';
    const EDITAR_GELADINHO_API_URL = '/geladinhos/editar';
    const DELETAR_GELADINHO_API_URL = '/geladinhos/deletar';

    // CSRF Token
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    let allIngredientes = []; // Lista de ingredientes carregados

    // Inicialização
    function init() {
        carregarIngredientes();
        configurarBotoesEditar();
        configurarFormularioEdicao();
        atualizarTabelaGeladinhos();

        // Configura o botão "Salvar" do novo ingrediente
        document.getElementById('salvarNovoIngredienteBtn').addEventListener('click', salvarNovoIngrediente);
    }

    document.querySelector('#novoIngredienteModal .btn-secondary-icl').addEventListener('click', function() {
        const novoIngredienteModal = bootstrap.Modal.getInstance(document.getElementById('novoIngredienteModal'));
        novoIngredienteModal.hide();
        const editarModal = new bootstrap.Modal(document.getElementById('editarGeladinhoModal'));
        editarModal.show();
    });

    function carregarIngredientes() {
        fetch(INGREDIENTES_API_URL)
            .then(response => response.json())
            .then(data => {
                allIngredientes = data;
                console.log('Ingredientes carregados:', allIngredientes);
            })
            .catch(error => console.error('Erro ao carregar ingredientes:', error));
    }

    function configurarBotoesEditar() {
        document.querySelectorAll('.btn-editar-geladinho').forEach(botao => {
            botao.addEventListener('click', () => abrirModalEdicao(botao));
        });
    }

    function configurarFormularioEdicao() {
        const formEditarGeladinho = document.getElementById('formEditarGeladinho');
        if (formEditarGeladinho) {
            formEditarGeladinho.addEventListener('submit', function (event) {
                event.preventDefault();
                enviarFormularioEdicao();
            });
        }

        const addIngredienteBtn = document.getElementById('addIngredienteBtnEditar');
        if (addIngredienteBtn) {
            addIngredienteBtn.addEventListener('click', addIngrediente);
        }
    }

    function abrirModalEdicao(botao) {
        const id = botao.getAttribute('data-id');
        console.log('Buscando detalhes do geladinho ID:', id);

        fetch(`/geladinhos/detalhes/${id}`)
            .then(response => response.json())
            .then(data => {
                console.log('Dados do geladinho recebidos:', data);
                preencherFormularioEdicao(data);
                new bootstrap.Modal(document.getElementById('editarGeladinhoModal')).show();
            })
            .catch(error => alert('Erro ao carregar detalhes do geladinho.'));
    }

    function preencherFormularioEdicao(data) {
        console.log('Preenchendo formulário com os dados do geladinho:', data);

        document.getElementById('editarGeladinhoId').value = data.id || '';
        document.getElementById('editarGeladinhoSabor').value = data.sabor || '';
        document.getElementById('editarGeladinhoEstoqueInicial').value = data.estoqueInicial || '';
        document.getElementById('editarGeladinhoEstoqueAtual').value = data.estoqueAtual || '';
        document.getElementById('editarGeladinhoPrecoCusto').value = parseFloat(data.precoCusto) || 0.0;
        document.getElementById('editarGeladinhoPrecoCustoUnitario').value = parseFloat(data.precoCustoUnitario) || 0.0;

        const ingredientesContainer = document.getElementById('editarIngredientesContainer');
        ingredientesContainer.innerHTML = '';

        if (data.ingredientes && Array.isArray(data.ingredientes)) {
            console.log('Ingredientes encontrados:', data.ingredientes);
            data.ingredientes.forEach((ingrediente, index) => {
                const newRow = criarLinhaIngrediente(index, ingrediente);
                ingredientesContainer.insertAdjacentHTML('beforeend', newRow);
            });
        } else {
            console.log('Nenhum ingrediente encontrado ou dados mal formatados.');
        }
    }

    function addIngrediente() {
        const ingredientesContainer = document.getElementById('editarIngredientesContainer');
        const newRow = criarLinhaIngrediente(ingredientesContainer.children.length, {
            nome: 'Novo Ingrediente',
            custoPorUnidade: 0,
            unidadeMedida: 'unidades',
            estoqueAtual: 0
        });
        ingredientesContainer.insertAdjacentHTML('beforeend', newRow);
    }

    function criarLinhaIngrediente(index, ingrediente) {
        return `
            <div class="ingrediente-item row g-2 mb-2" data-index="${index}">
                <div class="col-5">
                    <label>
                        <select class="form-select" name="ingredientes[${index}].ingredienteId" required>
                            <option value="">Selecione um ingrediente</option>
                            ${allIngredientes.map(ing => `
                                <option value="${ing.id}">${ing.nome} - ${ing.custoPorUnidade.toFixed(2)} R$ (${ing.unidadeMedida}, ${ing.estoqueAtual} em estoque)</option>
                            `).join('')}
                        </select>
                    </label>
                </div>
                <div class="col-5">
                    <label>
                        <input class="form-control"
                               type="number"
                               name="ingredientes[${index}].quantidade"
                               placeholder="Quantidade"
                               min="0"
                               step="0.1"
                               required>
                    </label>
                </div>
                <div class="col-2">
                    <i class="bi bi-x-circle-fill text-danger"
                       onclick="removerIngrediente(this)"
                       style="cursor:pointer;"></i>
                </div>
            </div>
        `;
    }

    function removerIngrediente(button) {
        button.closest('.ingrediente-item').remove();
    }

    function enviarFormularioEdicao() {
        const formEditarGeladinho = document.getElementById('formEditarGeladinho');
        const formData = new FormData(formEditarGeladinho);

        const ingredientes = [];
        document.querySelectorAll('.ingrediente-item').forEach((item, index) => {
            const ingredienteId = item.querySelector('select').value;
            const quantidade = item.querySelector('input').value;
            ingredientes.push({ ingredienteId, quantidade });
        });

        const data = {
            id: formData.get('id'),
            sabor: formData.get('sabor'),
            estoqueInicial: formData.get('estoqueInicial'),
            estoqueAtual: formData.get('estoqueAtual'),
            precoCusto: formData.get('precoCusto'),
            precoCustoUnitario: formData.get('precoCustoUnitario'),
            ingredientes: ingredientes
        };

        console.log('Enviando dados para atualizar geladinho:', data);

        fetch(EDITAR_GELADINHO_API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Erro ao salvar as alterações');
            }
            console.log('Resposta recebida do servidor para atualização de geladinho');
            return response.json();
        })
        .then(data => {
            console.log('Dados retornados após atualizar geladinho:', data);
            const toastElement = document.getElementById('successToast');
            if (!toastElement) {
                console.error('Elemento successToast não encontrado!');
                alert('Geladinho atualizado com sucesso!'); // Fallback para alert
                return;
            }
            const toastBody = toastElement.querySelector('.toast-body');
            if (!toastBody) {
                console.error('Elemento toast-body não encontrado!');
                alert('Geladinho atualizado com sucesso!'); // Fallback para alert
                return;
            }
            toastBody.textContent = 'Geladinho atualizado com sucesso!';
            const toast = new bootstrap.Toast(toastElement);
            console.log('Exibindo toast para geladinho atualizado');
            toast.show();
            atualizarTabelaGeladinhos();
            bootstrap.Modal.getInstance(document.getElementById('editarGeladinhoModal')).hide();
        })
        .catch(error => {
            alert('Erro ao salvar as alterações: ' + error.message);
            console.error('Detalhes do erro ao salvar geladinho:', error);
        });
    }

    function renderizarTabelaGeladinhos(data) {
        console.log('Renderizando tabela de geladinhos...');
        const tabelaGeladinhos = document.getElementById('tabelaGeladinhos');
        if (!tabelaGeladinhos) return;

        tabelaGeladinhos.innerHTML = '';
        if (!Array.isArray(data)) {
            console.error('Dados recebidos não são um array:', data);
            return;
        }

        data.forEach(geladinho => {
            const ingredientesHTML = geladinho.ingredientes && Array.isArray(geladinho.ingredientes)
                ? geladinho.ingredientes.map(ing =>
                    `<span class="badge bg-light text-dark me-1 fw-bold">${ing.nome}</span>`
                  ).join('')
                : '<span class="text-muted">Nenhum ingrediente</span>';

            const estoqueHTML = geladinho.estoqueAtual < 10
                ? `<span class="text-danger fw-bold">${geladinho.estoqueAtual}</span>`
                : geladinho.estoqueAtual;

            const row = `
                <tr>
                    <td>${geladinho.sabor || 'N/A'}</td>
                    <td class="mobile-hide">${ingredientesHTML}</td>
                    <td>${estoqueHTML}</td>
                    <td>${geladinho.precoCustoUnitario ? geladinho.precoCustoUnitario.toFixed(2) : 'N/A'}</td>
                    <td>
                        <button class="btn btn-warning btn-sm btn-editar-geladinho" data-id="${geladinho.id}">
                            <i class="bi bi-pencil"></i>
                            <span class="d-none d-md-inline">Editar</span>
                        </button>
                        <button class="btn btn-danger btn-sm btn-deletar-geladinho" data-id="${geladinho.id}">
                            <i class="bi bi-trash"></i>
                            <span class="d-none d-md-inline">Deletar</span>
                        </button>
                    </td>
                </tr>
            `;
            tabelaGeladinhos.insertAdjacentHTML('beforeend', row);
        });

        configurarBotoesEditar();
        configurarBotoesDeletar();
    }

    function configurarBotoesDeletar() {
        document.querySelectorAll('.btn-deletar-geladinho').forEach(botao => {
            botao.addEventListener('click', function () {
                const id = botao.getAttribute('data-id');
                if (confirm('Tem certeza que deseja deletar este geladinho?')) {
                    deletarGeladinho(id);
                }
            });
        });
    }

    function deletarGeladinho(id) {
        fetch(`${DELETAR_GELADINHO_API_URL}/${id}`, {
            method: 'DELETE',
            headers: {
                [csrfHeader]: csrfToken
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Erro ao deletar o geladinho');
            }
            return response.json();
        })
        .then(() => {
            alert('Geladinho deletado com sucesso!');
            atualizarTabelaGeladinhos();
        })
        .catch(error => {
            alert('Erro ao deletar geladinho.');
            console.error(error);
        });
    }

    function atualizarTabelaGeladinhos() {
        fetch(GELADINHOS_API_URL)
            .then(response => response.json())
            .then(data => renderizarTabelaGeladinhos(data))
            .catch(error => console.error('Erro ao atualizar a tabela:', error));
    }

    init();
});