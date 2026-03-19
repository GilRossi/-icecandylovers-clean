document.addEventListener('DOMContentLoaded', () => {
    const API_URLS = Object.freeze({
        PRODUTOS_CRIAR: '/api/produtos',
        INGREDIENTES_LISTAR: '/ingredientes/listar'
    });

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
    const ingredientesContainer = document.getElementById('ingredientesContainer');
    const formCadastroProduto = document.getElementById('formCadastroProduto');
    const adicionarIngredienteBtn = document.getElementById('adicionarIngredienteBtn');

    let ingredientesDisponiveis = [];

    function init() {
        if (!formCadastroProduto || !ingredientesContainer) {
            console.error('Elementos do cadastro de produto não encontrados.');
            return;
        }

        carregarIngredientesDisponiveis();
        configurarFormularioCadastro();
        adicionarIngredienteBtn?.addEventListener('click', adicionarLinhaIngrediente);
    }

    function carregarIngredientesDisponiveis() {
        fetch(API_URLS.INGREDIENTES_LISTAR)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Não foi possível carregar os ingredientes.');
                }
                return response.json();
            })
            .then(data => {
                ingredientesDisponiveis = Array.isArray(data) ? data : [];
            })
            .catch(error => {
                console.error('Erro ao carregar ingredientes:', error);
                showErrorToast(error.message);
            });
    }

    function configurarFormularioCadastro() {
        formCadastroProduto.addEventListener('submit', event => {
            event.preventDefault();

            const categoria = document.getElementById('categoria').value;
            const sabor = document.getElementById('sabor').value.trim();
            const estoqueInicial = parseInt(document.getElementById('estoqueInicial').value, 10) || 0;
            const estoqueAtual = parseInt(document.getElementById('estoqueAtual').value, 10) || 0;
            const precoCusto = parseFloat(document.getElementById('precoCusto').value);
            const ingredientes = coletarIngredientes();

            if (!sabor) {
                showErrorToast("O campo 'Sabor do Produto' é obrigatório.");
                return;
            }

            if (!categoria) {
                showErrorToast('Selecione a categoria do produto.');
                return;
            }

            if (estoqueInicial <= 0 || estoqueAtual < 0) {
                showErrorToast('Estoque inicial deve ser maior que zero e estoque atual não pode ser negativo.');
                return;
            }

            if (Number.isNaN(precoCusto) || precoCusto < 0) {
                showErrorToast('O preço de custo deve ser informado e não pode ser negativo.');
                return;
            }

            if (ingredientes.some(item => !item.ingredienteId || !(item.quantidade > 0))) {
                showErrorToast('Todos os ingredientes devem ter item selecionado e quantidade maior que zero.');
                return;
            }

            const jsonData = {
                sabor,
                estoqueInicial,
                estoqueAtual,
                precoCusto,
                categoria,
                ingredientes
            };

            fetch(API_URLS.PRODUTOS_CRIAR, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(jsonData)
            })
                .then(async response => {
                    if (response.ok) {
                        return response.json();
                    }

                    const errorData = await response.json().catch(() => ({}));
                    const message = errorData.erro || errorData.error || errorData.errors?.join(' ') || 'Erro ao cadastrar produto.';
                    throw new Error(message);
                })
                .then(() => {
                    showSuccessToast('Produto cadastrado com sucesso!');
                    formCadastroProduto.reset();
                    ingredientesContainer.innerHTML = '';
                })
                .catch(error => {
                    console.error('Erro ao cadastrar produto:', error);
                    showErrorToast(error.message);
                });
        });
    }

    function adicionarLinhaIngrediente() {
        const row = document.createElement('div');
        row.className = 'row g-2 align-items-end ingrediente-item';

        const selectCol = document.createElement('div');
        selectCol.className = 'col-md-7';

        const selectLabel = document.createElement('label');
        selectLabel.className = 'form-label w-100';
        selectLabel.textContent = 'Ingrediente';

        const select = document.createElement('select');
        select.className = 'form-select ingrediente-select';
        select.required = true;

        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = 'Selecione um ingrediente';
        select.appendChild(placeholder);

        ingredientesDisponiveis.forEach(ingrediente => {
            const option = document.createElement('option');
            option.value = String(ingrediente.id);
            option.textContent = `${ingrediente.nome} (${ingrediente.unidadeMedida}, estoque: ${ingrediente.estoqueAtual})`;
            select.appendChild(option);
        });

        selectLabel.appendChild(select);
        selectCol.appendChild(selectLabel);

        const quantidadeCol = document.createElement('div');
        quantidadeCol.className = 'col-md-4';

        const quantidadeLabel = document.createElement('label');
        quantidadeLabel.className = 'form-label w-100';
        quantidadeLabel.textContent = 'Quantidade';

        const quantidade = document.createElement('input');
        quantidade.type = 'number';
        quantidade.className = 'form-control ingrediente-quantidade';
        quantidade.min = '0.001';
        quantidade.step = '0.001';
        quantidade.required = true;

        quantidadeLabel.appendChild(quantidade);
        quantidadeCol.appendChild(quantidadeLabel);

        const acaoCol = document.createElement('div');
        acaoCol.className = 'col-md-1';

        const remover = document.createElement('button');
        remover.type = 'button';
        remover.className = 'btn btn-outline-danger w-100';
        remover.innerHTML = '<i class="bi bi-trash"></i>';
        remover.addEventListener('click', () => row.remove());
        acaoCol.appendChild(remover);

        row.appendChild(selectCol);
        row.appendChild(quantidadeCol);
        row.appendChild(acaoCol);
        ingredientesContainer.appendChild(row);
    }

    function coletarIngredientes() {
        return Array.from(document.querySelectorAll('.ingrediente-item')).map(item => ({
            ingredienteId: Number(item.querySelector('.ingrediente-select')?.value),
            quantidade: Number(item.querySelector('.ingrediente-quantidade')?.value)
        }));
    }

    function showSuccessToast(message) {
        const toastElement = document.getElementById('successToast');
        if (!toastElement) {
            alert(message);
            return;
        }

        toastElement.querySelector('.toast-body').textContent = message;
        new bootstrap.Toast(toastElement).show();
    }

    function showErrorToast(message) {
        const toastElement = document.getElementById('errorToast');
        if (!toastElement) {
            alert(message);
            return;
        }

        toastElement.querySelector('.toast-body').textContent = message;
        new bootstrap.Toast(toastElement).show();
    }

    init();
});
