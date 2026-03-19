package com.icecandylovers.controllers;

import com.icecandylovers.dtos.ProdutoDTO;
import com.icecandylovers.entities.CategoriaProduto;
import com.icecandylovers.exceptions.DuplicateResourceException;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.services.ProdutoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/produtos")
public class ProdutoController {

    private static final Logger logger = LoggerFactory.getLogger(ProdutoController.class);

    private final ProdutoService produtoService;

    @Autowired
    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    // Formulário para novo produto (HTML)
    @GetMapping("/novo")
    public String novoProduto(Model model, @RequestParam(required = false) String categoria) {
        logger.info("Carregando formulário para novo produto com categoria: {}", categoria);
        ProdutoDTO produtoDTO = new ProdutoDTO(null,
                "",
                0,
                0,
                null,
                null,
                categoria != null ? CategoriaProduto.valueOf(categoria.toUpperCase()) : null, List.of());
        model.addAttribute("produto", produtoDTO);
        model.addAttribute("currentUri", "/produtos/novo");
        return "cadastro-produto";
    }

    // Listar todos os produtos (versão única e robusta)
    @GetMapping
    public ResponseEntity<List<ProdutoDTO>> listarProdutos() {
        logger.info("Iniciando listagem de todos os produtos");
        try {
            List<ProdutoDTO> produtos = produtoService.listarProdutos();
            logger.debug("Produtos listados com sucesso, total: {}", produtos.size());
            return ResponseEntity.ok(produtos);
        } catch (Exception e) {
            logger.error("Erro ao listar produtos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of()); // Retorna lista vazia em caso de erro
        }
    }

    // Criar produto
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> criarProduto(@Valid @RequestBody ProdutoDTO produtoDTO) {
        logger.info("Criando novo produto: {}", produtoDTO.sabor());
        try {
            ProdutoDTO criado = produtoService.criarProduto(produtoDTO);
            logger.debug("Produto criado com sucesso: {}", criado);
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (DuplicateResourceException e) {
            logger.warn("Tentativa de criar produto duplicado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Produto já existe: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
            logger.warn("Recurso necessário não encontrado ao criar produto: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar produto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno ao criar produto"));
        }
    }

    // Buscar produto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> buscarProdutoPorId(@PathVariable Long id) {
        logger.info("Buscando produto com ID: {}", id);
        try {
            return produtoService.buscarProdutoPorId(id)
                    .map(produto -> {
                        logger.debug("Produto encontrado: {}", produto);
                        return ResponseEntity.ok(produto);
                    })
                    .orElseGet(() -> {
                        logger.warn("Produto não encontrado para ID: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Erro ao buscar produto por ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Atualizar produto
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarProduto(@PathVariable Long id, @Valid @RequestBody ProdutoDTO produtoDTO) {
        logger.info("Atualizando produto com ID: {}", id);
        try {
            if (!id.equals(produtoDTO.id())) {
                logger.warn("ID na URL ({}) não corresponde ao ID no corpo ({})", id, produtoDTO.id());
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "ID na URL não corresponde ao ID no corpo da requisição"));
            }
            ProdutoDTO atualizado = produtoService.atualizarProduto(produtoDTO);
            logger.debug("Produto atualizado com sucesso: {}", atualizado);
            return ResponseEntity.ok(atualizado);
        } catch (ResourceNotFoundException e) {
            logger.warn("Produto não encontrado para atualização: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Produto não encontrado: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao atualizar produto com ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno ao atualizar produto"));
        }
    }

    // Deletar produto
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarProduto(@PathVariable Long id) {
        logger.info("Iniciando deleção do produto com ID: {}", id);
        try {
            produtoService.deletarProduto(id);
            logger.debug("Produto com ID {} deletado com sucesso", id);
            return ResponseEntity.ok(Map.of("mensagem", "Produto deletado com sucesso"));
        } catch (IllegalStateException e) {
            logger.warn("Erro ao deletar produto devido a restrição: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Não é possível deletar: " + e.getMessage()));
        } catch (ResourceNotFoundException e) {
            logger.warn("Produto não encontrado para deleção: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Produto não encontrado: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao deletar produto com ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno ao deletar produto"));
        }
    }

    // Produzir produto
    @PostMapping("/{id}/produzir")
    public ResponseEntity<?> produzirProduto(@PathVariable Long id, @RequestParam int quantidade) {
        logger.info("Produzindo {} unidades do produto com ID: {}", quantidade, id);
        try {
            if (quantidade <= 0) {
                logger.warn("Quantidade inválida para produção: {}", quantidade);
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Quantidade deve ser maior que zero"));
            }
            produtoService.produzirProduto(id, quantidade);
            logger.debug("Produção de {} unidades do produto ID {} realizada com sucesso", quantidade, id);
            return ResponseEntity.ok(Map.of("mensagem", "Produção realizada com sucesso"));
        } catch (ResourceNotFoundException e) {
            logger.warn("Produto não encontrado para produção: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Produto não encontrado: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao produzir produto com ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno ao produzir produto"));
        }
    }

    // Calcular custo do produto
    @GetMapping("/{id}/custo")
    public ResponseEntity<BigDecimal> calcularCusto(@PathVariable Long id) {
        logger.info("Calculando custo do produto com ID: {}", id);
        try {
            BigDecimal custo = produtoService.calcularCusto(id);
            logger.debug("Custo calculado para produto ID {}: {}", id, custo);
            return ResponseEntity.ok(custo);
        } catch (ResourceNotFoundException e) {
            logger.warn("Produto não encontrado para cálculo de custo: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao calcular custo do produto com ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Listar produtos por categoria
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<?> listarPorCategoria(@PathVariable String categoria) {
        logger.info("Listando produtos da categoria: {}", categoria);
        try {
            String categoriaNormalizada = categoria.toUpperCase()
                    .replace(" ", "_")
                    .replace("Á", "A")
                    .replace("Ó", "O");
            CategoriaProduto categoriaEnum = CategoriaProduto.valueOf(categoriaNormalizada);
            List<ProdutoDTO> produtos = produtoService.listarProdutosPorCategoria(categoriaEnum);
            logger.debug("Produtos da categoria {} listados com sucesso, total: {}", categoria, produtos.size());
            return ResponseEntity.ok(produtos);
        } catch (IllegalArgumentException e) {
            logger.warn("Categoria inválida fornecida: {}", categoria);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("erro", "Categoria inválida: " + categoria));
        } catch (Exception e) {
            logger.error("Erro ao listar produtos por categoria {}: {}", categoria, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Erro interno ao listar por categoria"));
        }
    }
}