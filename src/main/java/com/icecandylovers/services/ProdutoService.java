package com.icecandylovers.services;

import com.icecandylovers.dtos.ProdutoDTO;
import com.icecandylovers.dtos.ProdutoIngredienteDTO;
import com.icecandylovers.entities.*;
import com.icecandylovers.exceptions.DuplicateResourceException;
import com.icecandylovers.exceptions.InsufficientStockException;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.IngredienteRepository;
import com.icecandylovers.repositories.LoteIngredienteRepository;
import com.icecandylovers.repositories.ProdutoRepository;
import com.icecandylovers.repositories.PromocaoRepository;
import com.icecandylovers.repositories.VendaItemRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    private static final Logger logger = LoggerFactory.getLogger(ProdutoService.class);

    private final ProdutoRepository produtoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final LoteIngredienteRepository loteIngredienteRepository;
    private final VendaItemRepository vendaItemRepository;
    private final PromocaoRepository promocaoRepository;
    private final IngredienteService ingredienteService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          IngredienteRepository ingredienteRepository,
                          LoteIngredienteRepository loteIngredienteRepository,
                          VendaItemRepository vendaItemRepository,
                          PromocaoRepository promocaoRepository,
                          IngredienteService ingredienteService) {
        this.produtoRepository = produtoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.loteIngredienteRepository = loteIngredienteRepository;
        this.vendaItemRepository = vendaItemRepository;
        this.promocaoRepository = promocaoRepository;
        this.ingredienteService = ingredienteService;
    }

    // Criar novo produto
    @Transactional
    public ProdutoDTO criarProduto(ProdutoDTO produtoDTO) {
        logger.info("Criando produto: {}", produtoDTO.sabor());

        Optional<Produto> existente = produtoRepository.findBySabor(produtoDTO.sabor());
        if (existente.isPresent()) {
            logger.warn("Tentativa de criar produto duplicado: {}", produtoDTO.sabor());
            throw new DuplicateResourceException("Já existe um produto com o sabor: " + produtoDTO.sabor());
        }

        Produto produto = new Produto();
        aplicarCamposBasicos(produto, produtoDTO);

        Produto salvo = produtoRepository.save(produto);
        atualizarIngredientes(salvo, produtoDTO);
        atualizarPrecoCustoUnitario(salvo);
        salvo = produtoRepository.save(salvo);
        logger.debug("Produto criado com sucesso: {}", salvo.getSabor());
        return toDTO(salvo);
    }

    // Atualizar produto existente
    @Transactional
    public ProdutoDTO atualizarProduto(ProdutoDTO dto) {
        logger.info("Atualizando produto com ID: {}", dto.id());

        Produto produto = produtoRepository.findByIdWithIngredientes(dto.id())
                .orElseThrow(() -> {
                    logger.warn("Produto não encontrado para atualização: {}", dto.id());
                    return new ResourceNotFoundException("Produto não encontrado: " + dto.id());
                });

        aplicarCamposBasicos(produto, dto);
        atualizarIngredientes(produto, dto);
        atualizarPrecoCustoUnitario(produto);

        Produto atualizado = produtoRepository.save(produto);
        logger.debug("Produto atualizado com sucesso: {}", atualizado.getSabor());
        return toDTO(atualizado);
    }

    // Deletar produto
    @Transactional
    public void deletarProduto(Long id) {
        logger.info("Deletando produto com ID: {}", id);

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Produto não encontrado para deleção: {}", id);
                    return new ResourceNotFoundException("Produto não encontrado: " + id);
                });
        if (vendaItemRepository.existsByProdutoId(id)) {
            logger.warn("Tentativa de deletar produto com vendas registradas: {}", produto.getSabor());
            throw new IllegalStateException("Não é possível deletar o produto '" + produto.getSabor() +
                    "' pois possui vendas registradas");
        }
        // Desassociar de promoções antes de deletar
        promocaoRepository.findAllByProdutoId(id).forEach(p -> p.getProdutos().remove(produto));
        promocaoRepository.flush();
        produtoRepository.delete(produto);
        logger.debug("Produto deletado com sucesso: {}", id);
    }

    // Listar todos os produtos
    public List<ProdutoDTO> listarProdutos() {
        logger.info("Listando todos os produtos");
        List<ProdutoDTO> produtos = produtoRepository.findAllWithIngredientes().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        logger.debug("Total de produtos listados: {}", produtos.size());
        return produtos;
    }

    // Buscar produto por ID (DTO)
    public Optional<ProdutoDTO> buscarProdutoPorId(Long id) {
        logger.info("Buscando produto por ID: {}", id);
        return produtoRepository.findByIdWithIngredientes(id).map(produto -> {
            logger.debug("Produto encontrado: {}", produto.getSabor());
            return toDTO(produto);
        });
    }

    // Buscar entidade Produto por ID
    public Optional<Produto> buscarEntidadeProdutoPorId(Long id) {
        logger.info("Buscando entidade Produto por ID: {}", id);
        return produtoRepository.findByIdWithIngredientes(id);
    }

    // Produzir produto
    @Transactional
    public void produzirProduto(Long produtoId, int quantidadeProduzida) {
        logger.info("Produzindo {} unidades do produto ID: {}", quantidadeProduzida, produtoId);

        if (quantidadeProduzida <= 0) {
            logger.warn("Quantidade inválida para produção: {}", quantidadeProduzida);
            throw new IllegalArgumentException("A quantidade a produzir deve ser maior que zero");
        }

        Produto produto = produtoRepository.findByIdWithIngredientes(produtoId)
                .orElseThrow(() -> {
                    logger.warn("Produto não encontrado para produção: {}", produtoId);
                    return new ResourceNotFoundException("Produto não encontrado: " + produtoId);
                });

        for (ProdutoIngrediente pi : produto.getIngredientes()) {
            BigDecimal quantidadeNecessaria = pi.getQuantidade().multiply(BigDecimal.valueOf(quantidadeProduzida));
            ingredienteService.consumirEstoque(pi.getIngrediente().getId(), quantidadeNecessaria);
        }

        produto.setEstoqueAtual(produto.getEstoqueAtual() + quantidadeProduzida);
        produtoRepository.save(produto);
        logger.debug("Produção concluída para produto ID: {}", produtoId);
    }

    // Calcular custo do produto
    @Transactional
    public BigDecimal calcularCusto(Long produtoId) {
        logger.info("Calculando custo do produto ID: {}", produtoId);

        Produto produto = produtoRepository.findByIdWithIngredientes(produtoId)
                .orElseThrow(() -> {
                    logger.warn("Produto não encontrado para cálculo de custo: {}", produtoId);
                    return new ResourceNotFoundException("Produto não encontrado: " + produtoId);
                });
        BigDecimal custo = produto.getIngredientes().stream()
                .map(pi -> pi.getLoteIngrediente().getCustoPorUnidade().multiply(pi.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        logger.debug("Custo calculado para produto ID {}: {}", produtoId, custo);
        return custo;
    }

    public int calcularTotalEstoque() {
        logger.info("Calculando total de estoque de todos os produtos");
        Integer total = produtoRepository.sumEstoqueAtual();
        int resultado = total != null ? total : 0;
        logger.debug("Total de estoque calculado: {}", resultado);
        return resultado;
    }

    // Listar produtos por categoria
    public List<ProdutoDTO> listarProdutosPorCategoria(CategoriaProduto categoria) {
        logger.info("Listando produtos da categoria: {}", categoria);
        List<ProdutoDTO> produtos = produtoRepository.findByCategoria(categoria).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        logger.debug("Total de produtos listados na categoria {}: {}", categoria, produtos.size());
        return produtos;
    }

    @Transactional
    public void decrementarEstoque(Long produtoId, Integer quantidade) {
        logger.info("Decrementando estoque do produto ID: {} em {}", produtoId, quantidade);

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> {
                    logger.warn("Produto não encontrado para decrementar estoque: {}", produtoId);
                    return new ResourceNotFoundException("Produto não encontrado: " + produtoId);
                });
        if (produto.getEstoqueAtual() < quantidade) {
            logger.warn("Estoque insuficiente para o produto: {}", produto.getSabor());
            throw new InsufficientStockException("Estoque insuficiente para o produto: " + produto.getSabor());
        }
        produto.setEstoqueAtual(produto.getEstoqueAtual() - quantidade);
        produtoRepository.save(produto);
        logger.debug("Estoque decrementado com sucesso para produto ID: {}", produtoId);
    }

    @Transactional
    public void ajustarEstoque(Long produtoId, int quantidade) {
        logger.info("Ajustando estoque do produto ID: {} em {}", produtoId, quantidade);

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> {
                    logger.warn("Produto não encontrado para ajustar estoque: {}", produtoId);
                    return new ResourceNotFoundException("Produto não encontrado: " + produtoId);
                });
        int novoEstoque = produto.getEstoqueAtual() + quantidade;
        if (novoEstoque < 0) {
            logger.warn("Estoque não pode ficar negativo para o produto: {}", produto.getSabor());
            throw new IllegalArgumentException("Estoque não pode ficar negativo para o produto: " + produto.getSabor());
        }
        produto.setEstoqueAtual(novoEstoque);
        produtoRepository.save(produto);
        logger.debug("Estoque ajustado com sucesso para produto ID: {}", produtoId);
    }

    // Listar produtos com estoque baixo (1 a 10 unidades)
    @Transactional
    public List<ProdutoDTO> listarProdutosEstoqueBaixo() {
        logger.info("Listando produtos com estoque baixo");
        return produtoRepository.findAllWithIngredientes().stream()
                .filter(p -> p.getEstoqueAtual() != null && p.getEstoqueAtual() > 0 && p.getEstoqueAtual() <= 10)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Converter Produto para DTO
    private ProdutoDTO toDTO(Produto produto) {
        logger.debug("Convertendo Produto para DTO: {}", produto.getSabor());

        List<ProdutoIngredienteDTO> ingredientesDTO = produto.getIngredientes().stream()
                .filter(pi -> pi.getIngrediente() != null)
                .map(pi -> new ProdutoIngredienteDTO(
                        pi.getIngrediente().getId(),
                        pi.getIngrediente().getNome(),
                        pi.getQuantidade().doubleValue()
                ))
                .collect(Collectors.toList());

        ProdutoDTO dto = new ProdutoDTO(
                produto.getId(),
                produto.getSabor(),
                produto.getEstoqueInicial(),
                produto.getEstoqueAtual(),
                produto.getPrecoCusto() != null ? produto.getPrecoCusto().doubleValue() : 0.0,
                produto.getPrecoCustoUnitario() != null ? produto.getPrecoCustoUnitario().doubleValue() : 0.0,
                produto.getCategoria(),
                ingredientesDTO
        );
        logger.debug("Conversão para DTO concluída com sucesso para produto: {}", produto.getSabor());
        return dto;
    }

    private void aplicarCamposBasicos(Produto produto, ProdutoDTO dto) {
        produto.setSabor(dto.sabor());
        produto.setEstoqueInicial(dto.estoqueInicial());
        produto.setEstoqueAtual(dto.estoqueAtual());
        produto.setPrecoCusto(dto.precoCusto() != null ? BigDecimal.valueOf(dto.precoCusto()) : BigDecimal.ZERO);
        produto.setCategoria(dto.categoria() != null ? dto.categoria() : CategoriaProduto.GELADINHO_GOURMET);
    }

    // Atualizar preco custo unitario
    private void atualizarPrecoCustoUnitario(Produto produto) {
        logger.debug("Atualizando preço custo unitário para produto: {}", produto.getSabor());
        if (produto.getEstoqueInicial() > 0 && produto.getPrecoCusto() != null) {
            produto.setPrecoCustoUnitario(produto.getPrecoCusto()
                    .divide(BigDecimal.valueOf(produto.getEstoqueInicial()), 2, RoundingMode.HALF_UP));
        } else {
            produto.setPrecoCustoUnitario(BigDecimal.ZERO);
        }
        logger.debug("Preço custo unitário atualizado: {}", produto.getPrecoCustoUnitario());
    }

    // Atualizar ingredientes do produto
    private void atualizarIngredientes(Produto produto, ProdutoDTO dto) {
        logger.debug("Atualizando ingredientes para produto: {}", produto.getSabor());

        if (!produto.getIngredientes().isEmpty()) {
            // Orphan removal marks the old links for deletion. Flush before recreating
            // the same composite keys to avoid "deleted object would be re-saved".
            produto.getIngredientes().clear();
            produtoRepository.flush();
        }

        if (dto.ingredientes() != null && !dto.ingredientes().isEmpty()) {
            List<ProdutoIngrediente> ingredientes = dto.ingredientes().stream()
                    .map(piDTO -> {
                        Ingrediente ingrediente = ingredienteRepository.findById(piDTO.ingredienteId())
                                .orElseThrow(() -> {
                                    logger.warn("Ingrediente não encontrado: {}", piDTO.ingredienteId());
                                    return new ResourceNotFoundException("Ingrediente não encontrado: " + piDTO.ingredienteId());
                                });
                        LoteIngrediente lote = loteIngredienteRepository.findTopByIngredienteIdOrderByDataCompraDesc(piDTO.ingredienteId())
                                .orElseThrow(() -> {
                                    logger.warn("Nenhum lote disponível para ingrediente: {}", piDTO.ingredienteId());
                                    return new ResourceNotFoundException("Nenhum lote disponível para ingrediente: " + piDTO.ingredienteId());
                                });
                        ProdutoIngrediente pi = new ProdutoIngrediente();
                        pi.setProduto(produto);
                        pi.setLoteIngrediente(lote);
                        pi.setQuantidade(BigDecimal.valueOf(piDTO.quantidade()));
                        return pi;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
            produto.getIngredientes().addAll(ingredientes);
            logger.debug("Ingredientes atualizados com sucesso para produto: {}", produto.getSabor());
        } else {
            logger.debug("Nenhum ingrediente fornecido para atualização no produto: {}", produto.getSabor());
        }
    }
}
