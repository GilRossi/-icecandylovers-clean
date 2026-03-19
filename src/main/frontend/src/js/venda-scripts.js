document.addEventListener('DOMContentLoaded', () => {
    console.log('Página de vendas carregada!');
    const escapeHtml = (value) => String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');

    const formVenda = document.getElementById('formVenda');
    if (!formVenda) {
        console.error('Formulário de venda não encontrado.');
        return;
    }

    console.log('Formulário de venda encontrado.');

    formVenda.addEventListener('submit', async (event) => {
        event.preventDefault();
        console.log('Formulário de venda submetido.');

        try {
            const formData = getFormData(formVenda);
            if (!validateFormData(formData)) {
                showAlert('Por favor, preencha todos os campos corretamente.', 'danger');
                return;
            }

            const vendaDTO = createVendaDTO(formData);
            const vendaId = document.getElementById('vendaId')?.value;
            const response = await sendVendaRequest(vendaDTO, vendaId);

            if (response.success) {
                showSuccessModal(response.totalVenda, vendaDTO.dataVenda, vendaId);
                await updateDashboard();
                await loadVendas();

                if (!vendaId) {
                    formVenda.reset();
                } else {
                    window.location.href = '/vendas/nova';
                }
            } else {
                throw new Error(response.error || 'Erro ao processar venda.');
            }
        } catch (error) {
            console.error('Erro ao processar venda:', error);
            showAlert(`Ocorreu um erro: ${error.message}`, 'danger');
        }
    });

    loadVendas();

    function getFormData(form) {
        return {
            produtoId: form.querySelector('[name="produtoId"]').value,
            quantidade: form.querySelector('[name="quantidade"]').value,
            vendido: form.querySelector('[name="vendido"]').value,
            valorUnitarioVenda: form.querySelector('[name="valorUnitarioVenda"]').value,
            formaPagamento: form.querySelector('[name="formaPagamento"]').value,
            dataVenda: form.querySelector('[name="dataVenda"]').value
        };
    }

    function validateFormData({ produtoId, quantidade, vendido, valorUnitarioVenda, formaPagamento, dataVenda }) {
        if (!produtoId || !quantidade || !vendido || !valorUnitarioVenda || !formaPagamento || !dataVenda) {
            return false;
        }

        const qty = parseInt(quantidade);
        const unitValue = parseFloat(valorUnitarioVenda);
        if (isNaN(qty) || qty < 1) {
            showAlert('Quantidade inválida! Deve ser pelo menos 1.', 'warning');
            return false;
        }
        if (isNaN(unitValue) || unitValue <= 0) {
            showAlert('Valor unitário inválido! Deve ser maior que zero.', 'warning');
            return false;
        }
        if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(dataVenda)) {
            showAlert('Formato de data inválido! Use o formato correto.', 'warning');
            return false;
        }
        return true;
    }

    function createVendaDTO(formData) {
        return {
            produtoId: parseInt(formData.produtoId),
            quantidade: parseInt(formData.quantidade),
            vendido: formData.vendido,
            valorUnitarioVenda: parseFloat(formData.valorUnitarioVenda),
            formaPagamento: formData.formaPagamento,
            dataVenda: `${formData.dataVenda}:00`
        };
    }

    async function sendVendaRequest(vendaDTO, vendaId = null) {
        const csrfToken = document.querySelector('meta[name="_csrf"]').content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

        const url = vendaId ? `/vendas/editar/${vendaId}` : '/vendas/nova';
        const method = vendaId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                [csrfHeader]: csrfToken,
                'Content-Type': 'application/json' // Alterado para JSON
            },
            body: JSON.stringify(vendaDTO) // Envia como JSON
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || `Erro ${response.status}: Falha na requisição`);
        }
        return data;
    }

    function showAlert(message, type = 'danger') {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.role = 'alert';
        const messageText = document.createTextNode(message);
        const closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.className = 'btn-close';
        closeButton.setAttribute('data-bs-dismiss', 'alert');
        closeButton.setAttribute('aria-label', 'Fechar');
        alertDiv.appendChild(messageText);
        alertDiv.appendChild(closeButton);
        formVenda.insertAdjacentElement('beforebegin', alertDiv);
        setTimeout(() => alertDiv.remove(), 5000);
    }

    function showSuccessModal(totalVenda, dataVenda, vendaId = null) {
        const isEdit = !!vendaId;
        const formattedDate = new Date(dataVenda).toLocaleDateString('pt-BR');
        const formattedValue = parseFloat(totalVenda).toFixed(2);

        const modalContainer = document.createElement('div');
        modalContainer.innerHTML = `
            <div class="modal fade" id="successModal" tabindex="-1" aria-labelledby="successModalLabel" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header" style="background-color: #9370DB; color: white;">
                            <h5 class="modal-title" id="successModalLabel">${isEdit ? 'Venda Atualizada!' : 'Venda Registrada!'}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
                        </div>
                        <div class="modal-body">
                            <p>Você vendeu no dia: ${formattedDate}.</p>
                            <p>R$ ${formattedValue}. </p>
                            <h6>Parabéns!</h6>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
                            ${isEdit ? '' : '<button type="button" class="btn btn-ice" onclick="window.location.href=\'/vendas/nova\'">Nova Venda</button>'}
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modalContainer);
        const modal = new bootstrap.Modal(modalContainer.querySelector('#successModal'));
        modal.show();

        modalContainer.addEventListener('hidden.bs.modal', () => modalContainer.remove());
    }

    async function updateDashboard() {
        try {
            const response = await fetch('/vendas/total-hoje');
            if (!response.ok) throw new Error('Erro ao buscar total de vendas');
            const data = await response.json();

            document.querySelectorAll('.venda-metric').forEach(element => {
                element.textContent = `R$ ${parseFloat(data.totalVendasHoje).toFixed(2)}`;
            });
        } catch (error) {
            console.error('Erro ao atualizar dashboard:', error);
            showAlert('Falha ao atualizar o dashboard.', 'warning');
        }
    }

    async function loadVendas() {
        try {
            const response = await fetch('/vendas/listar');
            if (!response.ok) throw new Error('Erro ao buscar vendas');
            const vendas = await response.json();

            const tbody = document.querySelector('#tabelaVendas tbody');
            tbody.innerHTML = '';

            vendas.forEach(venda => {
                const tr = document.createElement('tr');
                const total = (venda.quantidade * venda.valorUnitarioVenda).toFixed(2);
                tr.innerHTML = `
                    <td>${new Date(venda.dataVenda).toLocaleString('pt-BR')}</td>
                    <td>${escapeHtml(venda.produtoSabor || 'Desconhecido')}</td>
                    <td>${venda.quantidade}</td>
                    <td>${escapeHtml(venda.vendido)}</td>
                    <td>${escapeHtml(venda.formaPagamento)}</td>
                    <td>R$ ${parseFloat(venda.valorUnitarioVenda).toFixed(2)}</td>
                    <td>R$ ${total}</td>
                    <td>
                        <button class="btn btn-sm btn-primary me-1" onclick="editVenda('${escapeHtml(venda.id)}')">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteVenda('${escapeHtml(venda.id)}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } catch (error) {
            console.error('Erro ao carregar vendas:', error);
            showAlert('Falha ao carregar as vendas.', 'warning');
        }
    }

    window.editVenda = function(id) {
        window.location.href = `/vendas/editar/${id}`;
    };

    window.deleteVenda = async function(id) {
        if (!confirm('Tem certeza que deseja deletar esta venda?')) return;

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

            const response = await fetch(`/vendas/deletar/${id}`, {
                method: 'DELETE',
                headers: {
                    [csrfHeader]: csrfToken
                }
            });

            if (!response.ok) throw new Error('Erro ao deletar venda');
            showAlert('Venda deletada com sucesso!', 'success');
            await loadVendas();
        } catch (error) {
            console.error('Erro ao deletar venda:', error);
            showAlert(`Erro ao deletar venda: ${error.message}`, 'danger');
        }
    };
});
