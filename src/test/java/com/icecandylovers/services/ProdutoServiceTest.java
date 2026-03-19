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
import com.icecandylovers.repositories.VendaItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoService Unit Tests")
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private IngredienteRepository ingredienteRepository;

    @Mock
    private LoteIngredienteRepository loteIngredienteRepository;

    @Mock
    private VendaItemRepository vendaItemRepository;

    @Mock
    private IngredienteService ingredienteService;

    @InjectMocks
    private ProdutoService produtoService;

    // ---------------------------------------------------------------
    // Helper methods for creating test entities
    // ---------------------------------------------------------------

    private Produto createTestProduto(Long id, String sabor, int estoqueInicial, int estoqueAtual,
                                      BigDecimal precoCusto, CategoriaProduto categoria) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setSabor(sabor);
        produto.setEstoqueInicial(estoqueInicial);
        produto.setEstoqueAtual(estoqueAtual);
        produto.setPrecoCusto(precoCusto);
        produto.setCategoria(categoria);
        produto.setIngredientes(new ArrayList<>());

        if (estoqueInicial > 0 && precoCusto != null) {
            produto.setPrecoCustoUnitario(
                    precoCusto.divide(BigDecimal.valueOf(estoqueInicial), 2, RoundingMode.HALF_UP));
        } else {
            produto.setPrecoCustoUnitario(BigDecimal.ZERO);
        }
        return produto;
    }

    private Ingrediente createTestIngrediente(Long id, String nome, String unidadeMedida,
                                              BigDecimal custoPorUnidade, BigDecimal estoqueAtual) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(id);
        ingrediente.setNome(nome);
        ingrediente.setUnidadeMedida(unidadeMedida);
        ingrediente.setCustoPorUnidade(custoPorUnidade);
        ingrediente.setEstoqueAtual(estoqueAtual);
        ingrediente.setEstoqueInicial(estoqueAtual);
        return ingrediente;
    }

    private LoteIngrediente createTestLote(Long id, Ingrediente ingrediente, BigDecimal quantidade,
                                           BigDecimal custoPorUnidade) {
        LoteIngrediente lote = new LoteIngrediente();
        lote.setId(id);
        lote.setIngrediente(ingrediente);
        lote.setQuantidade(quantidade);
        lote.setQuantidadeAtual(quantidade);
        lote.setCustoPorUnidade(custoPorUnidade);
        lote.setDataCompra(LocalDate.now());
        lote.setQuantidadeComprada(quantidade);
        return lote;
    }

    private ProdutoIngrediente createTestProdutoIngrediente(Produto produto, LoteIngrediente lote,
                                                            BigDecimal quantidade) {
        ProdutoIngrediente pi = new ProdutoIngrediente();
        pi.setProduto(produto);
        pi.setLoteIngrediente(lote);
        pi.setQuantidade(quantidade);
        pi.setId(new ProdutoIngredienteId(produto.getId(), lote.getId()));
        return pi;
    }

    private ProdutoDTO createTestProdutoDTO(Long id, String sabor, int estoqueInicial, int estoqueAtual,
                                            double precoCusto, CategoriaProduto categoria,
                                            List<ProdutoIngredienteDTO> ingredientes) {
        double precoCustoUnitario = estoqueInicial > 0 ? precoCusto / estoqueInicial : 0.0;
        return new ProdutoDTO(id, sabor, estoqueInicial, estoqueAtual,
                precoCusto, precoCustoUnitario, categoria, ingredientes);
    }

    // ---------------------------------------------------------------
    // criarProduto
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("criarProduto")
    class CriarProdutoTests {

        @Test
        @DisplayName("should create product successfully and return DTO")
        void shouldCreateProductSuccessfullyAndReturnDTO() {
            // Arrange
            ProdutoIngredienteDTO piDTO = new ProdutoIngredienteDTO(1L, "Leite Condensado", 0.5);
            ProdutoDTO inputDTO = createTestProdutoDTO(null, "Morango Premium", 100, 100,
                    250.00, CategoriaProduto.GELADINHO_GOURMET, List.of(piDTO));

            Ingrediente ingrediente = createTestIngrediente(1L, "Leite Condensado", "L",
                    new BigDecimal("8.50"), new BigDecimal("50.000"));
            LoteIngrediente lote = createTestLote(10L, ingrediente, new BigDecimal("50.000"),
                    new BigDecimal("8.50"));

            when(produtoRepository.findBySabor("Morango Premium")).thenReturn(Optional.empty());
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
                Produto p = invocation.getArgument(0);
                if (p.getId() == null) {
                    p.setId(1L);
                }
                return p;
            });
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(ingrediente));
            when(loteIngredienteRepository.findTopByIngredienteIdOrderByDataCompraDesc(1L))
                    .thenReturn(Optional.of(lote));

            // Act
            ProdutoDTO result = produtoService.criarProduto(inputDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("Morango Premium", result.sabor());
            assertEquals(100, result.estoqueInicial());
            assertEquals(100, result.estoqueAtual());
            assertEquals(CategoriaProduto.GELADINHO_GOURMET, result.categoria());
            assertNotNull(result.ingredientes());
            assertEquals(1, result.ingredientes().size());
            assertEquals("Leite Condensado", result.ingredientes().get(0).nome());

            verify(produtoRepository).findBySabor("Morango Premium");
            verify(produtoRepository, times(2)).save(any(Produto.class));
            verify(ingredienteRepository).findById(1L);
            verify(loteIngredienteRepository).findTopByIngredienteIdOrderByDataCompraDesc(1L);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when sabor already exists")
        void shouldThrowDuplicateResourceExceptionWhenSaborAlreadyExists() {
            // Arrange
            ProdutoDTO inputDTO = createTestProdutoDTO(null, "Morango Premium", 100, 100,
                    250.00, CategoriaProduto.GELADINHO_GOURMET, Collections.emptyList());
            Produto existing = createTestProduto(1L, "Morango Premium", 100, 100,
                    new BigDecimal("250.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findBySabor("Morango Premium")).thenReturn(Optional.of(existing));

            // Act & Assert
            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                    () -> produtoService.criarProduto(inputDTO));

            assertTrue(exception.getMessage().contains("Morango Premium"));
            verify(produtoRepository).findBySabor("Morango Premium");
            verify(produtoRepository, never()).save(any(Produto.class));
        }

        @Test
        @DisplayName("should set default categoria to GELADINHO_GOURMET when null")
        void shouldSetDefaultCategoriaToGeladinhoGourmetWhenNull() {
            // Arrange
            ProdutoDTO inputDTO = new ProdutoDTO(null, "Limao Especial", 50, 50,
                    100.00, 2.0, null, Collections.emptyList());

            when(produtoRepository.findBySabor("Limao Especial")).thenReturn(Optional.empty());
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
                Produto p = invocation.getArgument(0);
                if (p.getId() == null) {
                    p.setId(2L);
                }
                return p;
            });

            // Act
            ProdutoDTO result = produtoService.criarProduto(inputDTO);

            // Assert
            assertNotNull(result);
            assertEquals(CategoriaProduto.GELADINHO_GOURMET, result.categoria());
            verify(produtoRepository, times(2)).save(any(Produto.class));
        }

        @Test
        @DisplayName("should calculate precoCustoUnitario correctly (precoCusto / estoqueInicial)")
        void shouldCalculatePrecoCustoUnitarioCorrectly() {
            // Arrange
            ProdutoDTO inputDTO = createTestProdutoDTO(null, "Chocolate Belga", 200, 200,
                    500.00, CategoriaProduto.GELADINHO_GOURMET, Collections.emptyList());

            when(produtoRepository.findBySabor("Chocolate Belga")).thenReturn(Optional.empty());
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
                Produto p = invocation.getArgument(0);
                if (p.getId() == null) {
                    p.setId(3L);
                }
                return p;
            });

            // Act
            ProdutoDTO result = produtoService.criarProduto(inputDTO);

            // Assert
            assertNotNull(result);
            // precoCustoUnitario = 500.00 / 200 = 2.50
            assertEquals(2.50, result.precoCustoUnitario(), 0.01);
            verify(produtoRepository, times(2)).save(any(Produto.class));
        }
    }

    // ---------------------------------------------------------------
    // atualizarProduto
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("atualizarProduto")
    class AtualizarProdutoTests {

        @Test
        @DisplayName("should update product fields correctly")
        void shouldUpdateProductFieldsCorrectly() {
            // Arrange
            Produto existingProduto = createTestProduto(1L, "Morango", 100, 80,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            ProdutoDTO updateDTO = createTestProdutoDTO(1L, "Morango Premium Atualizado", 150, 120,
                    300.00, CategoriaProduto.GELADINHO_ALCOOLICO, Collections.emptyList());

            when(produtoRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(existingProduto));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProdutoDTO result = produtoService.atualizarProduto(updateDTO);

            // Assert
            assertNotNull(result);
            assertEquals("Morango Premium Atualizado", result.sabor());
            assertEquals(150, result.estoqueInicial());
            assertEquals(120, result.estoqueAtual());
            assertEquals(CategoriaProduto.GELADINHO_ALCOOLICO, result.categoria());
            verify(produtoRepository).findByIdWithIngredientes(1L);
            verify(produtoRepository).save(any(Produto.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowResourceNotFoundExceptionWhenProductNotFound() {
            // Arrange
            ProdutoDTO updateDTO = createTestProdutoDTO(999L, "Inexistente", 50, 50,
                    100.00, CategoriaProduto.GELADINHO_GOURMET, Collections.emptyList());

            when(produtoRepository.findByIdWithIngredientes(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> produtoService.atualizarProduto(updateDTO));

            assertTrue(exception.getMessage().contains("999"));
            verify(produtoRepository).findByIdWithIngredientes(999L);
            verify(produtoRepository, never()).save(any(Produto.class));
        }
    }

    // ---------------------------------------------------------------
    // deletarProduto
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("deletarProduto")
    class DeletarProdutoTests {

        @Test
        @DisplayName("should delete product when no sales exist")
        void shouldDeleteProductWhenNoSalesExist() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango", 100, 50,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
            when(vendaItemRepository.existsByProdutoId(1L)).thenReturn(false);

            // Act
            produtoService.deletarProduto(1L);

            // Assert
            verify(produtoRepository).findById(1L);
            verify(vendaItemRepository).existsByProdutoId(1L);
            verify(produtoRepository).delete(produto);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            // Arrange
            when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> produtoService.deletarProduto(999L));

            assertTrue(exception.getMessage().contains("999"));
            verify(produtoRepository).findById(999L);
            verify(produtoRepository, never()).delete(any(Produto.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when product has sales")
        void shouldThrowIllegalStateExceptionWhenProductHasSales() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango com Vendas", 100, 50,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
            when(vendaItemRepository.existsByProdutoId(1L)).thenReturn(true);

            // Act & Assert
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> produtoService.deletarProduto(1L));

            assertTrue(exception.getMessage().contains("Morango com Vendas"));
            assertTrue(exception.getMessage().contains("vendas registradas"));
            verify(produtoRepository).findById(1L);
            verify(vendaItemRepository).existsByProdutoId(1L);
            verify(produtoRepository, never()).delete(any(Produto.class));
        }
    }

    // ---------------------------------------------------------------
    // listarProdutos
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("listarProdutos")
    class ListarProdutosTests {

        @Test
        @DisplayName("should return list of DTOs for all products")
        void shouldReturnListOfDTOsForAllProducts() {
            // Arrange
            Produto produto1 = createTestProduto(1L, "Morango", 100, 80,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);
            Produto produto2 = createTestProduto(2L, "Chocolate", 150, 120,
                    new BigDecimal("300.00"), CategoriaProduto.GELADINHO_ALCOOLICO);
            Produto produto3 = createTestProduto(3L, "Maracuja", 50, 50,
                    new BigDecimal("100.00"), CategoriaProduto.DOCINHO_GOURMET);

            when(produtoRepository.findAllWithIngredientes())
                    .thenReturn(List.of(produto1, produto2, produto3));

            // Act
            List<ProdutoDTO> result = produtoService.listarProdutos();

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("Morango", result.get(0).sabor());
            assertEquals("Chocolate", result.get(1).sabor());
            assertEquals("Maracuja", result.get(2).sabor());
            verify(produtoRepository).findAllWithIngredientes();
        }

        @Test
        @DisplayName("should return empty list when no products exist")
        void shouldReturnEmptyListWhenNoProductsExist() {
            // Arrange
            when(produtoRepository.findAllWithIngredientes()).thenReturn(Collections.emptyList());

            // Act
            List<ProdutoDTO> result = produtoService.listarProdutos();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(produtoRepository).findAllWithIngredientes();
        }
    }

    // ---------------------------------------------------------------
    // buscarProdutoPorId
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("buscarProdutoPorId")
    class BuscarProdutoPorIdTests {

        @Test
        @DisplayName("should return Optional with DTO when found")
        void shouldReturnOptionalWithDTOWhenFound() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango Premium", 100, 80,
                    new BigDecimal("250.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(produto));

            // Act
            Optional<ProdutoDTO> result = produtoService.buscarProdutoPorId(1L);

            // Assert
            assertTrue(result.isPresent());
            ProdutoDTO dto = result.get();
            assertEquals(1L, dto.id());
            assertEquals("Morango Premium", dto.sabor());
            assertEquals(100, dto.estoqueInicial());
            assertEquals(80, dto.estoqueAtual());
            assertEquals(250.00, dto.precoCusto(), 0.01);
            assertEquals(CategoriaProduto.GELADINHO_GOURMET, dto.categoria());
            verify(produtoRepository).findByIdWithIngredientes(1L);
        }

        @Test
        @DisplayName("should return empty Optional when not found")
        void shouldReturnEmptyOptionalWhenNotFound() {
            // Arrange
            when(produtoRepository.findByIdWithIngredientes(999L)).thenReturn(Optional.empty());

            // Act
            Optional<ProdutoDTO> result = produtoService.buscarProdutoPorId(999L);

            // Assert
            assertTrue(result.isEmpty());
            verify(produtoRepository).findByIdWithIngredientes(999L);
        }
    }

    // ---------------------------------------------------------------
    // produzirProduto
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("produzirProduto")
    class ProduzirProdutoTests {

        @Test
        @DisplayName("should produce product and increment stock")
        void shouldProduceProductAndIncrementStock() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango Premium", 100, 80,
                    new BigDecimal("250.00"), CategoriaProduto.GELADINHO_GOURMET);
            // Product with no ingredients for simplicity in this test
            when(produtoRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(produto));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            produtoService.produzirProduto(1L, 20);

            // Assert
            assertEquals(100, produto.getEstoqueAtual()); // 80 + 20
            verify(produtoRepository).findByIdWithIngredientes(1L);
            verify(produtoRepository).save(produto);
        }

        @Test
        @DisplayName("should consume ingredient stock for each ingredient")
        void shouldConsumeIngredientStockForEachIngredient() {
            // Arrange
            Ingrediente leite = createTestIngrediente(1L, "Leite Condensado", "L",
                    new BigDecimal("8.50"), new BigDecimal("100.000"));
            Ingrediente morango = createTestIngrediente(2L, "Morango", "kg",
                    new BigDecimal("12.00"), new BigDecimal("50.000"));

            LoteIngrediente loteLeite = createTestLote(10L, leite, new BigDecimal("100.000"),
                    new BigDecimal("8.50"));
            LoteIngrediente loteMorango = createTestLote(11L, morango, new BigDecimal("50.000"),
                    new BigDecimal("12.00"));

            Produto produto = createTestProduto(1L, "Morango Premium", 100, 80,
                    new BigDecimal("250.00"), CategoriaProduto.GELADINHO_GOURMET);

            ProdutoIngrediente piLeite = createTestProdutoIngrediente(produto, loteLeite, new BigDecimal("0.200"));
            ProdutoIngrediente piMorango = createTestProdutoIngrediente(produto, loteMorango, new BigDecimal("0.150"));
            produto.getIngredientes().add(piLeite);
            produto.getIngredientes().add(piMorango);

            when(produtoRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(produto));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

            int quantidadeProduzida = 10;

            // Act
            produtoService.produzirProduto(1L, quantidadeProduzida);

            // Assert
            // Leite: 0.200 * 10 = 2.000
            verify(ingredienteService).consumirEstoque(eq(1L), eq(new BigDecimal("2.000")));
            // Morango: 0.150 * 10 = 1.500
            verify(ingredienteService).consumirEstoque(eq(2L), eq(new BigDecimal("1.500")));
            assertEquals(90, produto.getEstoqueAtual()); // 80 + 10
            verify(produtoRepository).save(produto);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when quantity <= 0")
        void shouldThrowIllegalArgumentExceptionWhenQuantityIsZeroOrNegative() {
            // Act & Assert - zero
            IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class,
                    () -> produtoService.produzirProduto(1L, 0));
            assertTrue(exceptionZero.getMessage().contains("maior que zero"));

            // Act & Assert - negative
            IllegalArgumentException exceptionNeg = assertThrows(IllegalArgumentException.class,
                    () -> produtoService.produzirProduto(1L, -5));
            assertTrue(exceptionNeg.getMessage().contains("maior que zero"));

            verify(produtoRepository, never()).findByIdWithIngredientes(anyLong());
            verify(produtoRepository, never()).save(any(Produto.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowResourceNotFoundExceptionWhenProductNotFound() {
            // Arrange
            when(produtoRepository.findByIdWithIngredientes(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> produtoService.produzirProduto(999L, 10));

            assertTrue(exception.getMessage().contains("999"));
            verify(produtoRepository).findByIdWithIngredientes(999L);
            verify(produtoRepository, never()).save(any(Produto.class));
        }
    }

    // ---------------------------------------------------------------
    // calcularCusto
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("calcularCusto")
    class CalcularCustoTests {

        @Test
        @DisplayName("should calculate cost from all ingredients")
        void shouldCalculateCostFromAllIngredients() {
            // Arrange
            Ingrediente leite = createTestIngrediente(1L, "Leite Condensado", "L",
                    new BigDecimal("8.50"), new BigDecimal("100.000"));
            Ingrediente morango = createTestIngrediente(2L, "Morango", "kg",
                    new BigDecimal("12.00"), new BigDecimal("50.000"));

            LoteIngrediente loteLeite = createTestLote(10L, leite, new BigDecimal("100.000"),
                    new BigDecimal("8.50"));
            LoteIngrediente loteMorango = createTestLote(11L, morango, new BigDecimal("50.000"),
                    new BigDecimal("12.00"));

            Produto produto = createTestProduto(1L, "Morango Premium", 100, 80,
                    new BigDecimal("250.00"), CategoriaProduto.GELADINHO_GOURMET);

            // Leite: custoPorUnidade=8.50, quantidade=0.500 -> 4.250
            ProdutoIngrediente piLeite = createTestProdutoIngrediente(produto, loteLeite, new BigDecimal("0.500"));
            // Morango: custoPorUnidade=12.00, quantidade=0.300 -> 3.600
            ProdutoIngrediente piMorango = createTestProdutoIngrediente(produto, loteMorango, new BigDecimal("0.300"));

            produto.getIngredientes().add(piLeite);
            produto.getIngredientes().add(piMorango);

            when(produtoRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(produto));

            // Act
            BigDecimal custo = produtoService.calcularCusto(1L);

            // Assert
            // Expected: 8.50 * 0.500 + 12.00 * 0.300 = 4.25000 + 3.60000 = 7.85000
            assertEquals(0, new BigDecimal("7.85").compareTo(custo),
                    "Cost should be 7.85 (sum of custoPorUnidade * quantidade for each ingredient)");
            verify(produtoRepository).findByIdWithIngredientes(1L);
        }

        @Test
        @DisplayName("should return zero for product with no ingredients")
        void shouldReturnZeroForProductWithNoIngredients() {
            // Arrange
            Produto produto = createTestProduto(1L, "Simples", 50, 50,
                    new BigDecimal("50.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(produto));

            // Act
            BigDecimal custo = produtoService.calcularCusto(1L);

            // Assert
            assertEquals(0, BigDecimal.ZERO.compareTo(custo));
            verify(produtoRepository).findByIdWithIngredientes(1L);
        }
    }

    // ---------------------------------------------------------------
    // decrementarEstoque
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("decrementarEstoque")
    class DecrementarEstoqueTests {

        @Test
        @DisplayName("should decrement stock correctly")
        void shouldDecrementStockCorrectly() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango", 100, 80,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            produtoService.decrementarEstoque(1L, 30);

            // Assert
            assertEquals(50, produto.getEstoqueAtual()); // 80 - 30
            verify(produtoRepository).findById(1L);
            verify(produtoRepository).save(produto);
        }

        @Test
        @DisplayName("should throw InsufficientStockException when stock is less than quantity")
        void shouldThrowInsufficientStockExceptionWhenStockIsLessThanQuantity() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango Escasso", 100, 5,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

            // Act & Assert
            InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                    () -> produtoService.decrementarEstoque(1L, 10));

            assertTrue(exception.getMessage().contains("Morango Escasso"));
            assertEquals(5, produto.getEstoqueAtual()); // Stock unchanged
            verify(produtoRepository).findById(1L);
            verify(produtoRepository, never()).save(any(Produto.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowResourceNotFoundExceptionWhenProductNotFound() {
            // Arrange
            when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> produtoService.decrementarEstoque(999L, 10));

            assertTrue(exception.getMessage().contains("999"));
            verify(produtoRepository).findById(999L);
            verify(produtoRepository, never()).save(any(Produto.class));
        }
    }

    // ---------------------------------------------------------------
    // ajustarEstoque
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ajustarEstoque")
    class AjustarEstoqueTests {

        @Test
        @DisplayName("should adjust stock with positive value")
        void shouldAdjustStockWithPositiveValue() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango", 100, 50,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            produtoService.ajustarEstoque(1L, 25);

            // Assert
            assertEquals(75, produto.getEstoqueAtual()); // 50 + 25
            verify(produtoRepository).findById(1L);
            verify(produtoRepository).save(produto);
        }

        @Test
        @DisplayName("should adjust stock with negative value")
        void shouldAdjustStockWithNegativeValue() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango", 100, 50,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            produtoService.ajustarEstoque(1L, -20);

            // Assert
            assertEquals(30, produto.getEstoqueAtual()); // 50 - 20
            verify(produtoRepository).findById(1L);
            verify(produtoRepository).save(produto);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when result would be negative")
        void shouldThrowIllegalArgumentExceptionWhenResultWouldBeNegative() {
            // Arrange
            Produto produto = createTestProduto(1L, "Morango Baixo", 100, 10,
                    new BigDecimal("200.00"), CategoriaProduto.GELADINHO_GOURMET);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> produtoService.ajustarEstoque(1L, -15));

            assertTrue(exception.getMessage().contains("negativo"));
            assertTrue(exception.getMessage().contains("Morango Baixo"));
            assertEquals(10, produto.getEstoqueAtual()); // Stock unchanged
            verify(produtoRepository).findById(1L);
            verify(produtoRepository, never()).save(any(Produto.class));
        }
    }

    // ---------------------------------------------------------------
    // calcularTotalEstoque
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("calcularTotalEstoque")
    class CalcularTotalEstoqueTests {

        @Test
        @DisplayName("should return sum of all product stocks")
        void shouldReturnSumOfAllProductStocks() {
            // Arrange
            when(produtoRepository.sumEstoqueAtual()).thenReturn(350);

            // Act
            int total = produtoService.calcularTotalEstoque();

            // Assert
            assertEquals(350, total);
            verify(produtoRepository).sumEstoqueAtual();
        }

        @Test
        @DisplayName("should return 0 when no products exist")
        void shouldReturnZeroWhenNoProductsExist() {
            // Arrange
            when(produtoRepository.sumEstoqueAtual()).thenReturn(0);

            // Act
            int total = produtoService.calcularTotalEstoque();

            // Assert
            assertEquals(0, total);
            verify(produtoRepository).sumEstoqueAtual();
        }
    }
}
