package com.icecandylovers.services;

import com.icecandylovers.dtos.VendaDTO;
import com.icecandylovers.entities.*;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.VendaItemRepository;
import com.icecandylovers.repositories.VendaRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VendaService {

    private static final Logger logger = LoggerFactory.getLogger(VendaService.class);

    private final VendaRepository vendaRepository;
    private final VendaItemRepository vendaItemRepository;
    private final ProdutoService produtoService;

    public VendaService(VendaRepository vendaRepository, VendaItemRepository vendaItemRepository, ProdutoService produtoService) {
        this.vendaRepository = vendaRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.produtoService = produtoService;
    }

    // Buscar produto por ID (usando a entidade Produto)
    public Produto buscarProdutoPorId(Long produtoId) {
        logger.info("Buscando produto com ID: {}", produtoId);
        return produtoService.buscarEntidadeProdutoPorId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + produtoId));
    }

    // Obter as 10 vendas mais recentes
    public List<Venda> obterVendasRecentes() {
        logger.info("Obtendo as 10 vendas mais recentes");
        return vendaRepository.findTop10ByOrderByDataVendaDesc();
    }

    // Calcular total de vendas de hoje
    public BigDecimal calcularVendasHoje() {
        logger.info("Calculando total de vendas de hoje");
        BigDecimal total = vendaRepository.findTotalVendasHoje();
        return total != null ? total : BigDecimal.ZERO;
    }

    // Listar as 5 vendas mais recentes
    public List<Venda> listarVendasRecentes() {
        logger.info("Listando as 5 vendas mais recentes");
        return vendaRepository.findTop5ByOrderByDataVendaDesc();
    }

    // Registrar uma nova venda
    @Transactional
    public Venda registrarVenda(VendaDTO vendaDTO) {
        logger.info("Registrando venda para produto ID: {}", vendaDTO.produtoId());
        if (vendaDTO.produtoId() == null) {
            throw new IllegalArgumentException("ID do produto não pode ser nulo");
        }

        Produto produto = buscarProdutoPorId(vendaDTO.produtoId());
        produtoService.decrementarEstoque(vendaDTO.produtoId(), vendaDTO.quantidade());

        Venda venda = new Venda();
        venda.setDataVenda(vendaDTO.dataVenda() != null ? vendaDTO.dataVenda() : LocalDateTime.now());
        venda.setVendido(vendaDTO.vendido());
        venda.setValorUnitarioVenda(vendaDTO.valorUnitarioVenda());
        venda.setFormaPagamento(vendaDTO.formaPagamento());

        BigDecimal total = vendaDTO.valorUnitarioVenda().multiply(BigDecimal.valueOf(vendaDTO.quantidade()));
        venda.setTotal(total);

        vendaRepository.save(venda);

        VendaItem vendaItem = new VendaItem();
        vendaItem.setVenda(venda);
        vendaItem.setProduto(produto);
        vendaItem.setQuantidade(vendaDTO.quantidade());
        vendaItem.setValorUnitario(vendaDTO.valorUnitarioVenda());

        vendaItemRepository.save(vendaItem);
        logger.info("Venda registrada com sucesso, ID: {}", venda.getId());

        return venda;
    }

    // Buscar venda por ID
    public Venda buscarVendaPorId(Long id) {
        logger.info("Buscando venda com ID: {}", id);
        return vendaRepository.findByIdWithItens(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada com o ID: " + id));
    }

    // Atualizar uma venda existente
    @Transactional
    public Venda atualizarVenda(Long id, VendaDTO vendaDTO) {
        logger.info("Atualizando venda com ID: {}", id);
        Venda vendaExistente = buscarVendaPorId(id);
        if (vendaExistente.getItens().isEmpty()) {
            throw new ResourceNotFoundException("Venda não contém itens");
        }

        VendaItem itemExistente = vendaExistente.getItens().get(0);
        Long produtoAntigoId = itemExistente.getProduto().getId();
        int quantidadeAntiga = itemExistente.getQuantidade();

        if (!produtoAntigoId.equals(vendaDTO.produtoId())) {
            produtoService.ajustarEstoque(produtoAntigoId, quantidadeAntiga);
            Produto novoProduto = buscarProdutoPorId(vendaDTO.produtoId());
            produtoService.ajustarEstoque(vendaDTO.produtoId(), -vendaDTO.quantidade());
            itemExistente.setProduto(novoProduto);
        } else {
            int diferencaQuantidade = quantidadeAntiga - vendaDTO.quantidade();
            produtoService.ajustarEstoque(vendaDTO.produtoId(), diferencaQuantidade);
        }

        vendaExistente.setDataVenda(vendaDTO.dataVenda() != null ? vendaDTO.dataVenda() : LocalDateTime.now());
        vendaExistente.setVendido(vendaDTO.vendido());
        vendaExistente.setValorUnitarioVenda(vendaDTO.valorUnitarioVenda());
        vendaExistente.setFormaPagamento(vendaDTO.formaPagamento());

        BigDecimal total = vendaDTO.valorUnitarioVenda().multiply(BigDecimal.valueOf(vendaDTO.quantidade()));
        vendaExistente.setTotal(total);

        itemExistente.setQuantidade(vendaDTO.quantidade());
        itemExistente.setValorUnitario(vendaDTO.valorUnitarioVenda());

        Venda atualizada = vendaRepository.save(vendaExistente);
        logger.info("Venda atualizada com sucesso, ID: {}", atualizada.getId());

        return atualizada;
    }

    // Listar todas as vendas
    @Transactional
    public List<Venda> listarVendas() {
        logger.info("Listando todas as vendas");
        return vendaRepository.findAllWithItens();
    }

    // Deletar uma venda
    @Transactional
    public void deletarVenda(Long id) {
        logger.info("Deletando venda com ID: {}", id);
        Venda venda = buscarVendaPorId(id);
        VendaItem item = venda.getItens().get(0);
        produtoService.ajustarEstoque(item.getProduto().getId(), item.getQuantidade());
        vendaRepository.delete(venda);
        logger.info("Venda deletada com sucesso, ID: {}", id);
    }
}