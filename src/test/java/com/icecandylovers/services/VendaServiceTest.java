package com.icecandylovers.services;

import com.icecandylovers.dtos.VendaDTO;
import com.icecandylovers.entities.Produto;
import com.icecandylovers.entities.Venda;
import com.icecandylovers.entities.VendaItem;
import com.icecandylovers.entities.Vendido;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.VendaItemRepository;
import com.icecandylovers.repositories.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendaService Unit Tests")
class VendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private VendaItemRepository vendaItemRepository;

    @Mock
    private ProdutoService produtoService;

    @InjectMocks
    private VendaService vendaService;

    private Produto produto;
    private Produto outroProduto;
    private Venda vendaExistente;
    private VendaItem vendaItemExistente;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setSabor("Morango");
        produto.setEstoqueAtual(100);

        outroProduto = new Produto();
        outroProduto.setId(2L);
        outroProduto.setSabor("Limao");
        outroProduto.setEstoqueAtual(50);

        vendaItemExistente = new VendaItem();
        vendaItemExistente.setId(10L);
        vendaItemExistente.setProduto(produto);
        vendaItemExistente.setQuantidade(5);
        vendaItemExistente.setValorUnitario(new BigDecimal("3.50"));

        vendaExistente = new Venda();
        vendaExistente.setId(1L);
        vendaExistente.setDataVenda(LocalDateTime.of(2026, 3, 19, 10, 0));
        vendaExistente.setVendido(Vendido.PRAIA);
        vendaExistente.setValorUnitarioVenda(new BigDecimal("3.50"));
        vendaExistente.setFormaPagamento("PIX");
        vendaExistente.setTotal(new BigDecimal("17.50"));

        List<VendaItem> itens = new ArrayList<>();
        itens.add(vendaItemExistente);
        vendaExistente.setItens(itens);
        vendaItemExistente.setVenda(vendaExistente);
    }

    // -----------------------------------------------------------------------
    // Helper to build a VendaDTO record
    // -----------------------------------------------------------------------
    private VendaDTO buildVendaDTO(Long produtoId, Integer quantidade, Vendido vendido,
                                   BigDecimal valorUnitario, String formaPagamento,
                                   LocalDateTime dataVenda) {
        return new VendaDTO(produtoId, quantidade, vendido, valorUnitario, formaPagamento, dataVenda);
    }

    @Nested
    @DisplayName("registrarVenda")
    class RegistrarVenda {

        @Test
        @DisplayName("should register a sale successfully with correct total calculation")
        void shouldRegisterSaleSuccessfullyWithCorrectTotalCalculation() {
            // given
            LocalDateTime dataVenda = LocalDateTime.of(2026, 3, 19, 14, 30);
            VendaDTO dto = buildVendaDTO(1L, 3, Vendido.PRAIA,
                    new BigDecimal("5.00"), "PIX", dataVenda);

            when(produtoService.buscarEntidadeProdutoPorId(1L)).thenReturn(Optional.of(produto));
            doNothing().when(produtoService).decrementarEstoque(1L, 3);
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> {
                Venda v = invocation.getArgument(0);
                v.setId(100L);
                return v;
            });
            when(vendaItemRepository.save(any(VendaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Venda result = vendaService.registrarVenda(dto);

            // then
            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals(new BigDecimal("15.00"), result.getTotal()); // 5.00 * 3 = 15.00
            assertEquals(Vendido.PRAIA, result.getVendido());
            assertEquals(new BigDecimal("5.00"), result.getValorUnitarioVenda());
            assertEquals("PIX", result.getFormaPagamento());
            assertEquals(dataVenda, result.getDataVenda());

            verify(produtoService).decrementarEstoque(1L, 3);
            verify(vendaRepository).save(any(Venda.class));
            verify(vendaItemRepository).save(any(VendaItem.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when produtoId is null")
        void shouldThrowIllegalArgumentExceptionWhenProdutoIdIsNull() {
            // given
            VendaDTO dto = buildVendaDTO(null, 3, Vendido.PRAIA,
                    new BigDecimal("5.00"), "PIX", LocalDateTime.now());

            // when / then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> vendaService.registrarVenda(dto)
            );

            assertEquals("ID do produto n\u00e3o pode ser nulo", exception.getMessage());
            verify(vendaRepository, never()).save(any(Venda.class));
            verify(produtoService, never()).decrementarEstoque(any(), any());
        }

        @Test
        @DisplayName("should call decrementarEstoque with correct quantity")
        void shouldCallDecrementarEstoqueWithCorrectQuantity() {
            // given
            VendaDTO dto = buildVendaDTO(1L, 7, Vendido.EVENTO,
                    new BigDecimal("4.00"), "DINHEIRO", LocalDateTime.now());

            when(produtoService.buscarEntidadeProdutoPorId(1L)).thenReturn(Optional.of(produto));
            doNothing().when(produtoService).decrementarEstoque(1L, 7);
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> {
                Venda v = invocation.getArgument(0);
                v.setId(101L);
                return v;
            });
            when(vendaItemRepository.save(any(VendaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            vendaService.registrarVenda(dto);

            // then
            verify(produtoService, times(1)).decrementarEstoque(eq(1L), eq(7));
        }

        @Test
        @DisplayName("should set current datetime when dataVenda is null")
        void shouldSetCurrentDateTimeWhenDataVendaIsNull() {
            // given
            LocalDateTime beforeTest = LocalDateTime.now().minusSeconds(1);
            VendaDTO dto = buildVendaDTO(1L, 2, Vendido.ESTABELECIMENTO_PARCEIRO,
                    new BigDecimal("6.00"), "CARTAO", null);

            when(produtoService.buscarEntidadeProdutoPorId(1L)).thenReturn(Optional.of(produto));
            doNothing().when(produtoService).decrementarEstoque(1L, 2);
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> {
                Venda v = invocation.getArgument(0);
                v.setId(102L);
                return v;
            });
            when(vendaItemRepository.save(any(VendaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Venda result = vendaService.registrarVenda(dto);
            LocalDateTime afterTest = LocalDateTime.now().plusSeconds(1);

            // then
            assertNotNull(result.getDataVenda());
            assertTrue(result.getDataVenda().isAfter(beforeTest),
                    "dataVenda should be after the time before the test started");
            assertTrue(result.getDataVenda().isBefore(afterTest),
                    "dataVenda should be before the time after the test ended");
        }

        @Test
        @DisplayName("should save VendaItem with correct product and quantity")
        void shouldSaveVendaItemWithCorrectProductAndQuantity() {
            // given
            VendaDTO dto = buildVendaDTO(1L, 4, Vendido.PRAIA,
                    new BigDecimal("5.50"), "PIX", LocalDateTime.now());

            when(produtoService.buscarEntidadeProdutoPorId(1L)).thenReturn(Optional.of(produto));
            doNothing().when(produtoService).decrementarEstoque(1L, 4);
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> {
                Venda v = invocation.getArgument(0);
                v.setId(103L);
                return v;
            });
            when(vendaItemRepository.save(any(VendaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            vendaService.registrarVenda(dto);

            // then
            ArgumentCaptor<VendaItem> itemCaptor = ArgumentCaptor.forClass(VendaItem.class);
            verify(vendaItemRepository).save(itemCaptor.capture());

            VendaItem savedItem = itemCaptor.getValue();
            assertEquals(produto, savedItem.getProduto());
            assertEquals(4, savedItem.getQuantidade());
            assertEquals(new BigDecimal("5.50"), savedItem.getValorUnitario());
            assertNotNull(savedItem.getVenda());
        }
    }

    @Nested
    @DisplayName("atualizarVenda")
    class AtualizarVenda {

        @Test
        @DisplayName("should update sale when product changes (restores old stock, decrements new)")
        void shouldUpdateSaleWhenProductChanges() {
            // given
            Long vendaId = 1L;
            VendaDTO dto = buildVendaDTO(2L, 3, Vendido.EVENTO,
                    new BigDecimal("4.00"), "DINHEIRO", LocalDateTime.of(2026, 3, 19, 15, 0));

            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.of(vendaExistente));
            when(produtoService.buscarEntidadeProdutoPorId(2L)).thenReturn(Optional.of(outroProduto));
            doNothing().when(produtoService).ajustarEstoque(anyLong(), anyInt());
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Venda result = vendaService.atualizarVenda(vendaId, dto);

            // then
            // Restores old product stock (produtoId=1, quantity=+5)
            verify(produtoService).ajustarEstoque(eq(1L), eq(5));
            // Decrements new product stock (produtoId=2, quantity=-3)
            verify(produtoService).ajustarEstoque(eq(2L), eq(-3));

            assertEquals(new BigDecimal("12.00"), result.getTotal()); // 4.00 * 3
            assertEquals(Vendido.EVENTO, result.getVendido());
            assertEquals("DINHEIRO", result.getFormaPagamento());
            assertEquals(outroProduto, vendaItemExistente.getProduto());
            assertEquals(3, vendaItemExistente.getQuantidade());
        }

        @Test
        @DisplayName("should update sale when quantity changes on same product (adjusts stock difference)")
        void shouldUpdateSaleWhenQuantityChangesOnSameProduct() {
            // given
            Long vendaId = 1L;
            // Same product (id=1), but quantity changes from 5 to 8
            VendaDTO dto = buildVendaDTO(1L, 8, Vendido.PRAIA,
                    new BigDecimal("3.50"), "PIX", LocalDateTime.of(2026, 3, 19, 16, 0));

            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.of(vendaExistente));
            doNothing().when(produtoService).ajustarEstoque(anyLong(), anyInt());
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Venda result = vendaService.atualizarVenda(vendaId, dto);

            // then
            // diferencaQuantidade = 5 (old) - 8 (new) = -3 (need to decrement 3 more)
            verify(produtoService, times(1)).ajustarEstoque(eq(1L), eq(-3));

            assertEquals(new BigDecimal("28.00"), result.getTotal()); // 3.50 * 8
            assertEquals(8, vendaItemExistente.getQuantidade());
        }

        @Test
        @DisplayName("should update sale when quantity decreases on same product (restores stock)")
        void shouldUpdateSaleWhenQuantityDecreasesOnSameProduct() {
            // given
            Long vendaId = 1L;
            // Same product (id=1), quantity decreases from 5 to 2
            VendaDTO dto = buildVendaDTO(1L, 2, Vendido.PRAIA,
                    new BigDecimal("3.50"), "PIX", LocalDateTime.of(2026, 3, 19, 16, 0));

            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.of(vendaExistente));
            doNothing().when(produtoService).ajustarEstoque(anyLong(), anyInt());
            when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Venda result = vendaService.atualizarVenda(vendaId, dto);

            // then
            // diferencaQuantidade = 5 (old) - 2 (new) = +3 (restore 3 to stock)
            verify(produtoService, times(1)).ajustarEstoque(eq(1L), eq(3));

            assertEquals(new BigDecimal("7.00"), result.getTotal()); // 3.50 * 2
            assertEquals(2, vendaItemExistente.getQuantidade());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when venda not found")
        void shouldThrowResourceNotFoundExceptionWhenVendaNotFound() {
            // given
            Long vendaId = 999L;
            VendaDTO dto = buildVendaDTO(1L, 3, Vendido.PRAIA,
                    new BigDecimal("5.00"), "PIX", LocalDateTime.now());

            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.empty());

            // when / then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> vendaService.atualizarVenda(vendaId, dto)
            );

            assertTrue(exception.getMessage().contains("999"));
            verify(vendaRepository, never()).save(any(Venda.class));
            verify(produtoService, never()).ajustarEstoque(anyLong(), anyInt());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when venda has no items")
        void shouldThrowResourceNotFoundExceptionWhenVendaHasNoItems() {
            // given
            Long vendaId = 1L;
            Venda vendaSemItens = new Venda();
            vendaSemItens.setId(vendaId);
            vendaSemItens.setItens(Collections.emptyList());

            VendaDTO dto = buildVendaDTO(1L, 3, Vendido.PRAIA,
                    new BigDecimal("5.00"), "PIX", LocalDateTime.now());

            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.of(vendaSemItens));

            // when / then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> vendaService.atualizarVenda(vendaId, dto)
            );

            assertEquals("Venda n\u00e3o cont\u00e9m itens", exception.getMessage());
            verify(vendaRepository, never()).save(any(Venda.class));
            verify(produtoService, never()).ajustarEstoque(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("deletarVenda")
    class DeletarVenda {

        @Test
        @DisplayName("should delete venda and restore stock")
        void shouldDeleteVendaAndRestoreStock() {
            // given
            Long vendaId = 1L;
            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.of(vendaExistente));
            doNothing().when(produtoService).ajustarEstoque(anyLong(), anyInt());
            doNothing().when(vendaRepository).delete(any(Venda.class));

            // when
            vendaService.deletarVenda(vendaId);

            // then
            // Should restore 5 units of product 1 back to stock
            verify(produtoService).ajustarEstoque(eq(1L), eq(5));
            verify(vendaRepository).delete(vendaExistente);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when venda not found")
        void shouldThrowResourceNotFoundExceptionWhenVendaNotFound() {
            // given
            Long vendaId = 999L;
            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.empty());

            // when / then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> vendaService.deletarVenda(vendaId)
            );

            assertTrue(exception.getMessage().contains("999"));
            verify(vendaRepository, never()).delete(any(Venda.class));
            verify(produtoService, never()).ajustarEstoque(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("buscarVendaPorId")
    class BuscarVendaPorId {

        @Test
        @DisplayName("should return venda when found")
        void shouldReturnVendaWhenFound() {
            // given
            Long vendaId = 1L;
            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.of(vendaExistente));

            // when
            Venda result = vendaService.buscarVendaPorId(vendaId);

            // then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(Vendido.PRAIA, result.getVendido());
            assertEquals(new BigDecimal("17.50"), result.getTotal());
            verify(vendaRepository, times(1)).findByIdWithItens(vendaId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            // given
            Long vendaId = 999L;
            when(vendaRepository.findByIdWithItens(vendaId)).thenReturn(Optional.empty());

            // when / then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> vendaService.buscarVendaPorId(vendaId)
            );

            assertTrue(exception.getMessage().contains("999"));
            verify(vendaRepository, times(1)).findByIdWithItens(vendaId);
        }
    }

    @Nested
    @DisplayName("calcularVendasHoje")
    class CalcularVendasHoje {

        @Test
        @DisplayName("should return total when not null")
        void shouldReturnTotalWhenNotNull() {
            // given
            BigDecimal expectedTotal = new BigDecimal("250.75");
            when(vendaRepository.findTotalVendasHoje()).thenReturn(expectedTotal);

            // when
            BigDecimal result = vendaService.calcularVendasHoje();

            // then
            assertEquals(expectedTotal, result);
            verify(vendaRepository, times(1)).findTotalVendasHoje();
        }

        @Test
        @DisplayName("should return zero when null")
        void shouldReturnZeroWhenNull() {
            // given
            when(vendaRepository.findTotalVendasHoje()).thenReturn(null);

            // when
            BigDecimal result = vendaService.calcularVendasHoje();

            // then
            assertEquals(BigDecimal.ZERO, result);
            verify(vendaRepository, times(1)).findTotalVendasHoje();
        }
    }

    @Nested
    @DisplayName("listarVendas")
    class ListarVendas {

        @Test
        @DisplayName("should return all vendas with items")
        void shouldReturnAllVendasWithItems() {
            // given
            Venda venda2 = new Venda();
            venda2.setId(2L);
            venda2.setDataVenda(LocalDateTime.of(2026, 3, 18, 9, 0));
            venda2.setVendido(Vendido.EVENTO);
            venda2.setTotal(new BigDecimal("30.00"));

            VendaItem item2 = new VendaItem();
            item2.setId(20L);
            item2.setProduto(outroProduto);
            item2.setQuantidade(10);
            item2.setValorUnitario(new BigDecimal("3.00"));
            item2.setVenda(venda2);

            List<VendaItem> itens2 = new ArrayList<>();
            itens2.add(item2);
            venda2.setItens(itens2);

            List<Venda> vendas = List.of(vendaExistente, venda2);
            when(vendaRepository.findAllWithItens()).thenReturn(vendas);

            // when
            List<Venda> result = vendaService.listarVendas();

            // then
            assertNotNull(result);
            assertEquals(2, result.size());

            Venda firstVenda = result.get(0);
            assertEquals(1L, firstVenda.getId());
            assertFalse(firstVenda.getItens().isEmpty());
            assertEquals(1, firstVenda.getItens().size());

            Venda secondVenda = result.get(1);
            assertEquals(2L, secondVenda.getId());
            assertFalse(secondVenda.getItens().isEmpty());
            assertEquals(1, secondVenda.getItens().size());

            verify(vendaRepository, times(1)).findAllWithItens();
        }

        @Test
        @DisplayName("should return empty list when no vendas exist")
        void shouldReturnEmptyListWhenNoVendasExist() {
            // given
            when(vendaRepository.findAllWithItens()).thenReturn(Collections.emptyList());

            // when
            List<Venda> result = vendaService.listarVendas();

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(vendaRepository, times(1)).findAllWithItens();
        }
    }
}
