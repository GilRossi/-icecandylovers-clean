document.addEventListener('DOMContentLoaded', () => {
    const escapeHtml = (value) => String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');

    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    const successToast = new bootstrap.Toast(document.getElementById('successToast'));
    const errorToast = new bootstrap.Toast(document.getElementById('errorToast'));

    const fetchAPI = async (url, options = {}) => {
        try {
            console.log(`[fetchAPI] Enviando requisição: ${url}`, options);
            const response = await fetch(url, {
                headers: {
                    'Content-Type': 'application/json',
                    [header]: token,
                    ...options.headers
                },
                ...options
            });

            const contentType = response.headers.get('content-type');
            const isJSON = contentType && contentType.includes('application/json');
            const data = isJSON ? await response.json() : {};

            if (!response.ok) {
                throw new Error(data.error || `Erro HTTP: ${response.status}`);
            }

            console.log(`[fetchAPI] Resposta de ${url}:`, data);
            return data;
        } catch (error) {
            console.error(`[fetchAPI] Erro em ${url}:`, error);
            throw error;
        }
    };

    const showToast = (toast, message) => {
        toast._element.querySelector('.toast-body').textContent = message;
        toast.show();
    };

    const formatarEstoque = (valor, unidade) => {
        const v = parseFloat(valor);
        return unidade === 'unidade' ? v.toFixed(0) : v.toFixed(3);
    };

    const criarLinhaIngrediente = (ing) => {
        const tr = document.createElement('tr');
        tr.dataset.id = ing.id;
        tr.innerHTML = `
            <td>${escapeHtml(ing.id)}</td>
            <td>${escapeHtml(ing.nome)}</td>
            <td>${escapeHtml(ing.unidadeMedida)}</td>
            <td>${formatarEstoque(ing.estoqueAtual, ing.unidadeMedida)}</td>
            <td>R$ ${Number(ing.custoPorUnidade).toFixed(2)}</td>
            <td>
                <button class="btn btn-success btn-sm adicionar-lote" data-id="${escapeHtml(ing.id)}">
                    <i class="bi bi-plus-circle"></i> <span class="d-none d-md-inline">Lote</span>
                </button>
                <button class="btn btn-warning btn-sm editar-ingrediente" data-id="${escapeHtml(ing.id)}">
                    <i class="bi bi-pencil"></i> <span class="d-none d-md-inline">Editar</span>
                </button>
                <button class="btn btn-danger btn-sm deletar-ingrediente" data-id="${escapeHtml(ing.id)}">
                    <i class="bi bi-trash"></i> <span class="d-none d-md-inline">Deletar</span>
                </button>
            </td>
        `;
        return tr;
    };

    const adicionarIngredienteNaTabela = (ing) => {
        const tbody = document.querySelector('#tabelaIngredientes tbody');
        if (!tbody) return console.error('[DOM] <tbody> não encontrado!');
        tbody.appendChild(criarLinhaIngrediente(ing));
    };

    const atualizarIngredienteNaTabela = (ing) => {
        const tr = document.querySelector(`#tabelaIngredientes tbody tr[data-id="${ing.id}"]`);
        if (tr) {
            const novaLinha = criarLinhaIngrediente(ing);
            tr.replaceWith(novaLinha);
        } else {
            adicionarIngredienteNaTabela(ing);
        }
    };

    const removerIngredienteDaTabela = (id) => {
        const tr = document.querySelector(`#tabelaIngredientes tbody tr[data-id="${id}"]`);
        if (tr) tr.remove();
    };

    const listarIngredientes = async () => {
        try {
            const data = await fetchAPI('/ingredientes/listar');
            const tbody = document.querySelector('#tabelaIngredientes tbody');
            tbody.innerHTML = '';

            if (!data.length) {
                tbody.innerHTML = `<tr><td colspan="6" class="text-center">Nenhum ingrediente encontrado.</td></tr>`;
                return;
            }

            data.forEach(adicionarIngredienteNaTabela);
        } catch (err) {
            showToast(errorToast, `Erro ao listar ingredientes: ${err.message}`);
        }
    };

    document.getElementById('formNovoIngrediente').addEventListener('submit', async (e) => {
        e.preventDefault();

        const form = e.target;
        const formData = new FormData(form);
        const nome = formData.get('nome');
        const unidadeMedida = formData.get('unidadeMedida');
        const quantidade = parseFloat(formData.get('quantidade'));
        const precoTotal = parseFloat(formData.get('precoTotal'));

        if (!nome || !unidadeMedida || quantidade <= 0 || precoTotal < 0) {
            showToast(errorToast, 'Verifique os campos preenchidos.');
            return;
        }

        const payload = {
            ingrediente: {
                nome,
                unidadeMedida,
                estoqueInicial: quantidade,
                estoqueAtual: 0,
                custoPorUnidade: 0
            },
            lote: {
                quantidade,
                custoPorUnidade: precoTotal / quantidade
            }
        };

        try {
            const res = await fetchAPI('/ingredientes/salvar', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            adicionarIngredienteNaTabela(res.ingrediente);
            showToast(successToast, res.message || 'Ingrediente adicionado!');
            form.reset();
            bootstrap.Modal.getInstance(document.getElementById('novoIngredienteModal'))?.hide();
        } catch (err) {
            showToast(errorToast, `Erro: ${err.message}`);
            await listarIngredientes();
        }
    });

    document.getElementById('tabelaIngredientes').addEventListener('click', async (e) => {
        const btn = e.target.closest('button');
        if (!btn) return;

        const id = btn.dataset.id;

        if (btn.classList.contains('editar-ingrediente')) {
            try {
                const ing = await fetchAPI(`/ingredientes/${id}`);
                document.getElementById('editarIngredienteId').value = ing.id;
                document.getElementById('editarIngredienteNome').value = ing.nome;
                document.getElementById('editarIngredienteUnidade').value = ing.unidadeMedida;
                document.getElementById('editarIngredienteEstoque').value = ing.estoqueAtual;
                document.getElementById('editarIngredienteCusto').value = ing.custoPorUnidade;
                new bootstrap.Modal(document.getElementById('editarIngredienteModal')).show();
            } catch (err) {
                showToast(errorToast, `Erro ao carregar ingrediente: ${err.message}`);
            }
        }

        if (btn.classList.contains('deletar-ingrediente')) {
            if (confirm('Deseja deletar este ingrediente?')) {
                try {
                    const res = await fetchAPI(`/ingredientes/deletar/${id}`, { method: 'DELETE' });
                    removerIngredienteDaTabela(id);
                    showToast(successToast, res.message || 'Ingrediente deletado');
                } catch (err) {
                    showToast(errorToast, `Erro ao deletar: ${err.message}`);
                    await listarIngredientes();
                }
            }
        }

        if (btn.classList.contains('adicionar-lote')) {
            document.getElementById('loteIngredienteId').value = id;
            new bootstrap.Modal(document.getElementById('adicionarLoteModal')).show();
        }
    });

    document.getElementById('formEditarIngrediente').addEventListener('submit', async (e) => {
        e.preventDefault();

        const form = e.target;
        const formData = new FormData(form);

        const payload = {
            id: formData.get('id'),
            nome: formData.get('nome'),
            unidadeMedida: formData.get('unidadeMedida'),
            estoqueAtual: parseFloat(formData.get('estoqueAtual')),
            custoPorUnidade: parseFloat(formData.get('custoPorUnidade'))
        };

        if (payload.estoqueAtual < 0 || payload.custoPorUnidade < 0) {
            showToast(errorToast, 'Valores negativos não são permitidos.');
            return;
        }

        try {
            const res = await fetchAPI('/ingredientes/editar', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            atualizarIngredienteNaTabela(res.ingrediente);
            showToast(successToast, res.message || 'Ingrediente atualizado!');
            bootstrap.Modal.getInstance(document.getElementById('editarIngredienteModal')).hide();
        } catch (err) {
            showToast(errorToast, `Erro ao editar: ${err.message}`);
            await listarIngredientes();
        }
    });

    document.getElementById('salvarLoteBtn').addEventListener('click', async () => {
        const form = document.getElementById('formAdicionarLote');
        const formData = new FormData(form);

        const quantidade = parseFloat(formData.get('quantidade'));
        const valorTotal = parseFloat(formData.get('valorTotal'));
        const id = formData.get('ingredienteId');

        if (quantidade <= 0 || valorTotal < 0) {
            showToast(errorToast, 'Valores inválidos para lote.');
            return;
        }

        try {
            const res = await fetchAPI(`/ingredientes/${id}/adicionar-lote?quantidade=${quantidade}&valorTotal=${valorTotal}`, {
                method: 'POST'
            });

            if (res.ingrediente) {
                atualizarIngredienteNaTabela(res.ingrediente);
            }

            showToast(successToast, res.message || 'Lote adicionado!');
            bootstrap.Modal.getInstance(document.getElementById('adicionarLoteModal')).hide();
        } catch (err) {
            showToast(errorToast, `Erro ao adicionar lote: ${err.message}`);
            await listarIngredientes();
        }
    });

    listarIngredientes();
});
