/*********************
 * CONFIGURAÇÕES GERAIS
 *********************/
const CONFIG = {
  PRECOS: {
    GELADINHO_GOURMET: 15,
    GELADINHO_ALCOOLICO: 15,
    DOCINHO_GOURMET: 20
  },
  MAPEAMENTO_PRODUTOS: {
    'Geladinhos Gourmet': 'GELADINHO_GOURMET',
    'Geladinhos Alcoólicos': 'GELADINHO_ALCOOLICO',
    'Docinhos Gourmet': 'DOCINHO_GOURMET'
  },
  ENDPOINTS: {
    PRODUTOS: '/api/produtos/categoria',
    VENDAS: '/vendas/nova',
    CHAT: '/api/chat/message'
  }
};

/*********************
 * ESTADO E ELEMENTOS
 *********************/
let categoriaAtual = '';
let produtoAtual = null;
const escapeHtml = (value) => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;');
const elementos = {
  chat: {
    container: null, // Inicializado depois
    input: null,
    send: null,
    messages: null
  },
  modais: {
    venda: {
      container: null,
      produtoId: null,
      produtoNome: null,
      precoUnitario: null,
      quantidade: null,
      total: null,
      formaPagamento: null
    }
    // modais.produtos comentado no original, não será usado por enquanto
  }
};

// Obter tokens CSRF
let csrfToken, csrfHeader;

/*********************
 * GERENCIAMENTO DE PRODUTOS
 *********************/
const ProductManager = {
  async carregarProdutos(categoria) {
    // Como o modal de produtos está comentado, esta função não será usada por enquanto
    console.warn('carregarProdutos não implementado devido a modal comentado');
    return [];
  },

  exibirProdutos(produtos) {
    console.warn('exibirProdutos não implementado devido a modal comentado');
  },

  criarCardProduto(produto) {
    return ''; // Placeholder, não usado por enquanto
  }
};

/*********************
 * GERENCIAMENTO DE VENDAS
 *********************/
const VendaManager = {
  async abrirModalVenda(botao) {
    try {
      const preco = parseFloat(botao.getAttribute('data-preco'));
      const produtoId = botao.getAttribute('data-produto-id');
      const produtoNome = botao.getAttribute('data-produto-nome');

      const response = await fetch(`${CONFIG.ENDPOINTS.PRODUTOS}/${categoriaAtual}/${produtoId}`);
      if (!response.ok) throw new Error('Erro ao buscar produto');
      produtoAtual = await response.json();

      elementos.modais.venda.produtoId.value = produtoId;
      elementos.modais.venda.produtoNome.value = produtoNome;
      elementos.modais.venda.precoUnitario.value = preco;
      elementos.modais.venda.quantidade.value = 1;
      this.atualizarTotal();

      new bootstrap.Modal(elementos.modais.venda.container).show();
      console.log(`Modal de venda aberto para ${produtoNome}`);
    } catch (error) {
      console.error('Erro ao abrir modal de venda:', error);
      alert(error.message);
    }
  },

  atualizarTotal() {
    const quantidade = elementos.modais.venda.quantidade.value || 0;
    const preco = elementos.modais.venda.precoUnitario.value || 0;
    elementos.modais.venda.total.value = (quantidade * preco).toFixed(2);
  },

  async finalizarVenda() {
    const quantidade = parseInt(elementos.modais.venda.quantidade.value);

    if (quantidade > produtoAtual.estoqueAtual) {
      alert('Estoque insuficiente!');
      return;
    }

    const dadosVenda = {
      produtoId: elementos.modais.venda.produtoId.value,
      quantidade: quantidade,
      valorUnitarioVenda: elementos.modais.venda.precoUnitario.value,
      formaPagamento: elementos.modais.venda.formaPagamento.value,
      vendido: categoriaAtual === 'GELADINHO_ALCOOLICO' ? 'EVENTO' : 'PRAIA'
    };

    try {
      const response = await fetch(CONFIG.ENDPOINTS.VENDAS, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [csrfHeader]: csrfToken
        },
        body: JSON.stringify(dadosVenda)
      });

      if (!response.ok) throw new Error(await response.text());

      this.resetarModal();
      console.log('Venda finalizada com sucesso:', dadosVenda);
      alert(`Venda realizada! Total: R$ ${dadosVenda.quantidade * dadosVenda.valorUnitarioVenda}`);
    } catch (error) {
      console.error('Erro na venda:', error);
      alert(`Erro: ${error.message}`);
    }
  },

  resetarModal() {
    elementos.modais.venda.container.querySelector('.btn-close')?.click();
  }
};

/*********************
 * GERENCIAMENTO DE CHAT
 *********************/
const ChatManager = {
  toggleChat() {
    if (!elementos.chat.container) {
      console.error('Chat container não encontrado');
      return;
    }
    const isVisible = elementos.chat.container.style.display !== 'none';
    elementos.chat.container.style.display = isVisible ? 'none' : 'block';
    console.log(`Chat ${isVisible ? 'fechado' : 'aberto'}`);
    if (!isVisible) elementos.chat.input?.focus();
  },

  closeChat() {
    if (!elementos.chat.container) {
      console.error('Chat container não encontrado');
      return;
    }
    elementos.chat.container.style.display = 'none';
    console.log('Chat fechado pelo botão de fechar');
  },

  async sendMessage() {
    const message = elementos.chat.input?.value.trim();
    if (!message) return;

    this.disableUI(true);
    this.addMessage('Você', message);

    try {
      const response = await this.processarMensagem(message);
      this.addMessage('Gelyto', response);
    } catch (error) {
      console.error('Erro ao enviar mensagem:', error);
      this.addMessage('Gelyto', 'Ops! Tente novamente mais tarde.');
    } finally {
      this.disableUI(false);
    }
  },

  async processarMensagem(message) {
    const response = await fetch(CONFIG.ENDPOINTS.CHAT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    });

    if (!response.ok) throw new Error('Erro no chat: ' + response.status);
    return await response.text();
  },

  addMessage(sender, text) {
    if (!elementos.chat.messages) return;
    const messageDiv = document.createElement('div');
    const senderLabel = document.createElement('strong');
    senderLabel.textContent = `${sender}: `;
    const messageText = document.createTextNode(text);
    messageDiv.appendChild(senderLabel);
    messageDiv.appendChild(messageText);
    messageDiv.style.color = sender === 'Gelyto' ? '#9370DB' : '#333';
    elementos.chat.messages.appendChild(messageDiv);
    elementos.chat.messages.scrollTop = elementos.chat.messages.scrollHeight;
  },

  disableUI(disabled) {
    if (elementos.chat.input) elementos.chat.input.disabled = disabled;
    if (elementos.chat.send) elementos.chat.send.disabled = disabled;
    if (!disabled && elementos.chat.input) elementos.chat.input.value = '';
  }
};

/*********************
 * INICIALIZAÇÃO
 *********************/
document.addEventListener('DOMContentLoaded', () => {
  console.log('Inicializando aplicação...');

  // Inicializar elementos
  elementos.chat.container = document.getElementById('chat-container');
  elementos.chat.input = document.getElementById('chat-input');
  elementos.chat.send = document.getElementById('chat-send');
  elementos.chat.messages = document.getElementById('chat-messages');
  elementos.modais.venda.container = document.getElementById('vendaModal');
  elementos.modais.venda.produtoId = document.getElementById('produtoId');
  elementos.modais.venda.produtoNome = document.getElementById('produtoNome');
  elementos.modais.venda.precoUnitario = document.getElementById('precoUnitario');
  elementos.modais.venda.quantidade = document.getElementById('quantidade');
  elementos.modais.venda.total = document.getElementById('totalVenda');
  elementos.modais.venda.formaPagamento = document.getElementById('formaPagamento');

  csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  // Verificar se elementos essenciais foram encontrados
  if (!elementos.chat.container || !document.getElementById('chat-toggle')) {
    console.error('Elementos essenciais do chat não encontrados');
    return;
  }

  // Adicionar eventos do chat
  document.getElementById('chat-toggle').addEventListener('click', () => ChatManager.toggleChat());
  document.getElementById('chat-close').addEventListener('click', () => ChatManager.closeChat());
  elementos.chat.send?.addEventListener('click', () => ChatManager.sendMessage());
  elementos.chat.input?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') ChatManager.sendMessage();
  });

  // Adicionar eventos dos cartões de produto
  document.querySelectorAll('.cartao-produto').forEach(card => {
    card.addEventListener('click', async () => {
      const categoriaTexto = card.querySelector('h3')?.textContent.trim();
      categoriaAtual = CONFIG.MAPEAMENTO_PRODUTOS[categoriaTexto];

      if (!categoriaAtual) {
        console.error('Categoria não reconhecida:', categoriaTexto);
        alert('Categoria não reconhecida!');
        return;
      }

      console.log(`Categoria selecionada: ${categoriaTexto} (${categoriaAtual})`);
      // Modal de produtos comentado, então esta funcionalidade está desativada por enquanto
    });
  });

  // Evento do modal de venda
  elementos.modais.venda.quantidade?.addEventListener('input', () => VendaManager.atualizarTotal());

  // Expor função global
  window.finalizarVenda = () => VendaManager.finalizarVenda();
});
