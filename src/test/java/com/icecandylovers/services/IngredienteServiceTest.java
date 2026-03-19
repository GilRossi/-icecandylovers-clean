package com.icecandylovers.services;

import com.icecandylovers.entities.Ingrediente;
import com.icecandylovers.entities.LoteIngrediente;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.IngredienteRepository;
import com.icecandylovers.repositories.LoteIngredienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IngredienteService Unit Tests")
class IngredienteServiceTest {

    @Mock
    private IngredienteRepository ingredienteRepository;

    @Mock
    private LoteIngredienteRepository loteIngredienteRepository;

    @InjectMocks
    private IngredienteService ingredienteService;

    @Captor
    private ArgumentCaptor<Ingrediente> ingredienteCaptor;

    @Captor
    private ArgumentCaptor<LoteIngrediente> loteCaptor;

    @Captor
    private ArgumentCaptor<List<LoteIngrediente>> lotesListCaptor;

    private Ingrediente criarIngrediente(Long id, String nome, String unidadeMedida,
                                          BigDecimal estoqueAtual, BigDecimal custoPorUnidade) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(id);
        ingrediente.setNome(nome);
        ingrediente.setUnidadeMedida(unidadeMedida);
        ingrediente.setEstoqueAtual(estoqueAtual);
        ingrediente.setCustoPorUnidade(custoPorUnidade);
        return ingrediente;
    }

    private LoteIngrediente criarLote(Long id, Ingrediente ingrediente, BigDecimal quantidade,
                                       BigDecimal quantidadeAtual, BigDecimal custoPorUnidade,
                                       LocalDate dataCompra) {
        LoteIngrediente lote = new LoteIngrediente();
        lote.setId(id);
        lote.setIngrediente(ingrediente);
        lote.setQuantidade(quantidade);
        lote.setQuantidadeAtual(quantidadeAtual);
        lote.setCustoPorUnidade(custoPorUnidade);
        lote.setDataCompra(dataCompra);
        lote.setQuantidadeComprada(quantidade);
        return lote;
    }

    // =========================================================================
    // salvar
    // =========================================================================
    @Nested
    @DisplayName("salvar")
    class Salvar {

        @Test
        @DisplayName("should save and return ingrediente")
        void shouldSaveAndReturnIngrediente() {
            Ingrediente ingrediente = criarIngrediente(null, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.50"));

            Ingrediente salvo = criarIngrediente(1L, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.50"));

            when(ingredienteRepository.save(ingrediente)).thenReturn(salvo);

            Ingrediente resultado = ingredienteService.salvar(ingrediente);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Acucar", resultado.getNome());
            assertEquals("kg", resultado.getUnidadeMedida());
            assertEquals(BigDecimal.TEN, resultado.getEstoqueAtual());
            assertEquals(new BigDecimal("3.50"), resultado.getCustoPorUnidade());
            verify(ingredienteRepository, times(1)).save(ingrediente);
        }
    }

    // =========================================================================
    // criarComLoteInicial
    // =========================================================================
    @Nested
    @DisplayName("criarComLoteInicial")
    class CriarComLoteInicial {

        @Test
        @DisplayName("should create ingrediente with initial lot and return updated entity")
        void shouldCreateIngredienteWithInitialLotAndReturnUpdatedEntity() {
            String nome = "Leite Condensado";
            String unidadeMedida = "litro";
            BigDecimal quantidadeLote = new BigDecimal("50");
            BigDecimal custoPorUnidadeLote = new BigDecimal("4.00");

            // First save returns the ingredient with id
            Ingrediente ingredienteSalvo = criarIngrediente(1L, nome, unidadeMedida,
                    BigDecimal.ZERO, BigDecimal.ZERO);
            ingredienteSalvo.setEstoqueInicial(quantidadeLote);

            // After adicionarLote updates it, findById returns the updated version
            Ingrediente ingredienteAtualizado = criarIngrediente(1L, nome, unidadeMedida,
                    new BigDecimal("50"), new BigDecimal("4.00"));
            ingredienteAtualizado.setEstoqueInicial(quantidadeLote);

            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenReturn(ingredienteSalvo)   // first save (criarComLoteInicial)
                    .thenReturn(ingredienteAtualizado); // second save (adicionarLote)
            when(ingredienteRepository.findById(1L))
                    .thenReturn(Optional.of(ingredienteSalvo))    // findById inside adicionarLote
                    .thenReturn(Optional.of(ingredienteAtualizado)); // findById at the end of criarComLoteInicial
            when(loteIngredienteRepository.save(any(LoteIngrediente.class)))
                    .thenAnswer(invocation -> {
                        LoteIngrediente lote = invocation.getArgument(0);
                        lote.setId(10L);
                        return lote;
                    });

            Ingrediente resultado = ingredienteService.criarComLoteInicial(nome, unidadeMedida,
                    quantidadeLote, custoPorUnidadeLote);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals(nome, resultado.getNome());
            assertEquals(new BigDecimal("50"), resultado.getEstoqueAtual());
            assertEquals(new BigDecimal("4.00"), resultado.getCustoPorUnidade());

            // Verify ingrediente was saved (initial save + adicionarLote save)
            verify(ingredienteRepository, times(2)).save(any(Ingrediente.class));
            // Verify lote was saved
            verify(loteIngredienteRepository, times(1)).save(any(LoteIngrediente.class));
        }

        @Test
        @DisplayName("should set initial stock to zero and estoque from lote")
        void shouldSetInitialStockToZeroAndEstoqueFromLote() {
            String nome = "Morango";
            String unidadeMedida = "kg";
            BigDecimal quantidadeLote = new BigDecimal("30");
            BigDecimal custoPorUnidadeLote = new BigDecimal("12.00");

            Ingrediente ingredienteSalvo = criarIngrediente(2L, nome, unidadeMedida,
                    BigDecimal.ZERO, BigDecimal.ZERO);

            Ingrediente ingredienteAtualizado = criarIngrediente(2L, nome, unidadeMedida,
                    new BigDecimal("30"), new BigDecimal("12.00"));

            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenReturn(ingredienteSalvo)
                    .thenReturn(ingredienteAtualizado);
            when(ingredienteRepository.findById(2L))
                    .thenReturn(Optional.of(ingredienteSalvo))
                    .thenReturn(Optional.of(ingredienteAtualizado));
            when(loteIngredienteRepository.save(any(LoteIngrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ingredienteService.criarComLoteInicial(nome, unidadeMedida,
                    quantidadeLote, custoPorUnidadeLote);

            // Capture the first save to verify initial estoque was set to ZERO
            verify(ingredienteRepository, atLeast(1)).save(ingredienteCaptor.capture());
            Ingrediente primeiroSave = ingredienteCaptor.getAllValues().get(0);
            assertEquals(0, primeiroSave.getEstoqueAtual().compareTo(BigDecimal.ZERO),
                    "estoqueAtual should be ZERO before lote is added");
            assertEquals(0, primeiroSave.getCustoPorUnidade().compareTo(BigDecimal.ZERO),
                    "custoPorUnidade should be ZERO before lote is added");
        }
    }

    // =========================================================================
    // deletar
    // =========================================================================
    @Nested
    @DisplayName("deletar")
    class Deletar {

        @Test
        @DisplayName("should delete ingrediente and its lotes")
        void shouldDeleteIngredienteAndItsLotes() {
            Long id = 1L;
            Ingrediente ingrediente = criarIngrediente(id, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.50"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(ingrediente));

            ingredienteService.deletar(id);

            verify(loteIngredienteRepository, times(1)).deleteByIngredienteId(id);
            verify(ingredienteRepository, times(1)).delete(ingrediente);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            Long id = 999L;
            when(ingredienteRepository.findById(id)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> ingredienteService.deletar(id)
            );

            assertTrue(exception.getMessage().contains("999"));
            verify(loteIngredienteRepository, never()).deleteByIngredienteId(anyLong());
            verify(ingredienteRepository, never()).delete(any(Ingrediente.class));
        }
    }

    // =========================================================================
    // editar (3-param overload)
    // =========================================================================
    @Nested
    @DisplayName("editar (3-param: nome, unidadeMedida, estoqueAtual)")
    class Editar3Param {

        @Test
        @DisplayName("should update nome, unidadeMedida, estoqueAtual")
        void shouldUpdateNomeUnidadeMedidaEstoqueAtual() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.50"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Ingrediente resultado = ingredienteService.editar(id, "Acucar Refinado", "gramas",
                    new BigDecimal("20"));

            assertEquals("Acucar Refinado", resultado.getNome());
            assertEquals("gramas", resultado.getUnidadeMedida());
            assertEquals(new BigDecimal("20"), resultado.getEstoqueAtual());
            verify(ingredienteRepository).save(ingredienteCaptor.capture());
            Ingrediente capturado = ingredienteCaptor.getValue();
            assertEquals("Acucar Refinado", capturado.getNome());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            Long id = 999L;
            when(ingredienteRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> ingredienteService.editar(id, "Novo Nome", "kg", BigDecimal.TEN)
            );
            verify(ingredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when estoqueAtual is negative")
        void shouldThrowIllegalArgumentExceptionWhenEstoqueAtualIsNegative() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.50"));
            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.editar(id, "Acucar", "kg", new BigDecimal("-5"))
            );

            assertTrue(exception.getMessage().contains("negativo"));
            verify(ingredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not update fields when they are null")
        void shouldNotUpdateFieldsWhenTheyAreNull() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.50"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Ingrediente resultado = ingredienteService.editar(id, null, null, null);

            assertEquals("Acucar", resultado.getNome());
            assertEquals("kg", resultado.getUnidadeMedida());
            assertEquals(BigDecimal.TEN, resultado.getEstoqueAtual());
        }
    }

    // =========================================================================
    // editar (5-param overload)
    // =========================================================================
    @Nested
    @DisplayName("editar (5-param: nome, custoPorUnidade, unidadeMedida, estoqueAtual)")
    class Editar5Param {

        @Test
        @DisplayName("should update all fields including custoPorUnidade")
        void shouldUpdateAllFieldsIncludingCustoPorUnidade() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Leite", "litro",
                    new BigDecimal("50"), new BigDecimal("2.00"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Ingrediente resultado = ingredienteService.editar(id, "Leite Integral",
                    new BigDecimal("3.50"), "ml", new BigDecimal("100"));

            assertEquals("Leite Integral", resultado.getNome());
            assertEquals(new BigDecimal("3.50"), resultado.getCustoPorUnidade());
            assertEquals("ml", resultado.getUnidadeMedida());
            assertEquals(new BigDecimal("100"), resultado.getEstoqueAtual());
            verify(ingredienteRepository).save(any(Ingrediente.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when custoPorUnidade is negative")
        void shouldThrowIllegalArgumentExceptionWhenCustoPorUnidadeIsNegative() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Leite", "litro",
                    new BigDecimal("50"), new BigDecimal("2.00"));
            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.editar(id, "Leite", new BigDecimal("-1.00"),
                            "litro", new BigDecimal("50"))
            );

            assertTrue(exception.getMessage().contains("custo por unidade"));
            verify(ingredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when estoqueAtual is negative in 5-param overload")
        void shouldThrowIllegalArgumentExceptionWhenEstoqueAtualIsNegativeIn5Param() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Leite", "litro",
                    new BigDecimal("50"), new BigDecimal("2.00"));
            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.editar(id, "Leite", new BigDecimal("2.00"),
                            "litro", new BigDecimal("-10"))
            );

            assertTrue(exception.getMessage().contains("negativo"));
            verify(ingredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not update fields when they are null in 5-param overload")
        void shouldNotUpdateFieldsWhenTheyAreNullIn5Param() {
            Long id = 1L;
            Ingrediente existente = criarIngrediente(id, "Leite", "litro",
                    new BigDecimal("50"), new BigDecimal("2.00"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Ingrediente resultado = ingredienteService.editar(id, null, null, null, null);

            assertEquals("Leite", resultado.getNome());
            assertEquals(new BigDecimal("2.00"), resultado.getCustoPorUnidade());
            assertEquals("litro", resultado.getUnidadeMedida());
            assertEquals(new BigDecimal("50"), resultado.getEstoqueAtual());
        }
    }

    // =========================================================================
    // adicionarLote
    // =========================================================================
    @Nested
    @DisplayName("adicionarLote")
    class AdicionarLote {

        @Test
        @DisplayName("should add lot, update estoque and calculate weighted average cost correctly")
        void shouldAddLotUpdateEstoqueAndCalculateWeightedAverageCost() {
            Long ingredienteId = 1L;
            // Existing: stock 10 at cost 5.00
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Acucar", "kg",
                    new BigDecimal("10"), new BigDecimal("5.00"));

            // New lot: 20 at cost 8.00
            BigDecimal quantidade = new BigDecimal("20");
            BigDecimal custoPorUnidade = new BigDecimal("8.00");

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(loteIngredienteRepository.save(any(LoteIngrediente.class)))
                    .thenAnswer(invocation -> {
                        LoteIngrediente lote = invocation.getArgument(0);
                        lote.setId(10L);
                        return lote;
                    });

            LoteIngrediente resultado = ingredienteService.adicionarLote(ingredienteId,
                    quantidade, custoPorUnidade);

            // Verify weighted average: (10*5 + 20*8) / 30 = (50 + 160) / 30 = 210/30 = 7.00
            verify(ingredienteRepository).save(ingredienteCaptor.capture());
            Ingrediente capturado = ingredienteCaptor.getValue();
            assertEquals(0, new BigDecimal("30").compareTo(capturado.getEstoqueAtual()),
                    "New stock should be 10 + 20 = 30");
            assertEquals(0, new BigDecimal("7.00").compareTo(capturado.getCustoPorUnidade()),
                    "Weighted average should be (10*5 + 20*8) / 30 = 7.00");

            // Verify the lot was created correctly
            verify(loteIngredienteRepository).save(loteCaptor.capture());
            LoteIngrediente loteSalvo = loteCaptor.getValue();
            assertEquals(ingrediente, loteSalvo.getIngrediente());
            assertEquals(0, quantidade.compareTo(loteSalvo.getQuantidade()));
            assertEquals(0, quantidade.compareTo(loteSalvo.getQuantidadeAtual()));
            assertEquals(0, custoPorUnidade.compareTo(loteSalvo.getCustoPorUnidade()));
        }

        @Test
        @DisplayName("should calculate weighted average: (estoqueAtual*custoAtual + quantidade*custoNovo) / novoEstoque")
        void shouldCalculateWeightedAverageCorrectly() {
            Long ingredienteId = 1L;
            // Scenario: stock 10 at cost 5.00, adding 20 at cost 8.00
            // Expected: (10*5 + 20*8) / (10+20) = 210/30 = 7.00
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Farinha", "kg",
                    new BigDecimal("10"), new BigDecimal("5.00"));

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(loteIngredienteRepository.save(any(LoteIngrediente.class)))
                    .thenAnswer(invocation -> {
                        LoteIngrediente lote = invocation.getArgument(0);
                        lote.setId(20L);
                        return lote;
                    });

            ingredienteService.adicionarLote(ingredienteId, new BigDecimal("20"), new BigDecimal("8.00"));

            verify(ingredienteRepository).save(ingredienteCaptor.capture());
            Ingrediente capturado = ingredienteCaptor.getValue();

            // (10 * 5.00 + 20 * 8.00) / 30 = (50 + 160) / 30 = 210 / 30 = 7.00
            BigDecimal expectedCost = new BigDecimal("210").divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
            assertEquals(0, expectedCost.compareTo(capturado.getCustoPorUnidade()),
                    "Weighted average cost should be 7.00");
            assertEquals(0, new BigDecimal("30").compareTo(capturado.getEstoqueAtual()),
                    "Estoque should be 30");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when quantidade <= 0")
        void shouldThrowIllegalArgumentExceptionWhenQuantidadeIsZeroOrNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.adicionarLote(1L, BigDecimal.ZERO, new BigDecimal("5.00"))
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.adicionarLote(1L, new BigDecimal("-1"), new BigDecimal("5.00"))
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.adicionarLote(1L, null, new BigDecimal("5.00"))
            );

            verify(ingredienteRepository, never()).findById(anyLong());
            verify(loteIngredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when custoPorUnidade < 0")
        void shouldThrowIllegalArgumentExceptionWhenCustoPorUnidadeIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.adicionarLote(1L, new BigDecimal("10"), new BigDecimal("-1"))
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.adicionarLote(1L, new BigDecimal("10"), null)
            );

            verify(ingredienteRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when ingrediente not found")
        void shouldThrowResourceNotFoundExceptionWhenIngredienteNotFound() {
            Long ingredienteId = 999L;
            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> ingredienteService.adicionarLote(ingredienteId,
                            new BigDecimal("10"), new BigDecimal("5.00"))
            );

            verify(loteIngredienteRepository, never()).save(any());
        }
    }

    // =========================================================================
    // adicionarLotePorValorTotal
    // =========================================================================
    @Nested
    @DisplayName("adicionarLotePorValorTotal")
    class AdicionarLotePorValorTotal {

        @Test
        @DisplayName("deve_calcular_custoPorUnidade_corretamente_quando_valorTotal100_quantidade20")
        void deve_calcular_custoPorUnidade_corretamente_quando_valorTotal100_quantidade20() {
            // DADO: ingrediente existente, valorTotal=100, quantidade=20 => custoPorUnidade=5.00
            Long ingredienteId = 1L;
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Leite", "litro",
                    BigDecimal.ZERO, BigDecimal.ZERO);

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(loteIngredienteRepository.save(any(LoteIngrediente.class)))
                    .thenAnswer(invocation -> {
                        LoteIngrediente lote = invocation.getArgument(0);
                        lote.setId(1L);
                        return lote;
                    });

            BigDecimal quantidade = new BigDecimal("20");
            BigDecimal valorTotal = new BigDecimal("100");

            // QUANDO
            LoteIngrediente resultado = ingredienteService.adicionarLotePorValorTotal(ingredienteId, quantidade, valorTotal);

            // ENTÃO: custoPorUnidade deve ser 5.00 (100/20)
            verify(loteIngredienteRepository).save(loteCaptor.capture());
            LoteIngrediente loteSalvo = loteCaptor.getValue();
            assertEquals(0, new BigDecimal("5.00").compareTo(loteSalvo.getCustoPorUnidade()),
                    "custoPorUnidade deve ser 100/20 = 5.00");
            assertNotNull(resultado);
        }

        @Test
        @DisplayName("deve_lancar_IllegalArgumentException_quando_quantidade_zero")
        void deve_lancar_IllegalArgumentException_quando_quantidade_zero() {
            // DADO: quantidade = 0
            // QUANDO / ENTÃO: deve lançar IllegalArgumentException
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.adicionarLotePorValorTotal(1L, BigDecimal.ZERO, new BigDecimal("100"))
            );

            assertTrue(exception.getMessage().contains("maior que zero"));
            verify(ingredienteRepository, never()).findById(anyLong());
            verify(loteIngredienteRepository, never()).save(any());
        }
    }

    // =========================================================================
    // consumirEstoque (CRITICAL - FIFO business rule)
    // =========================================================================
    @Nested
    @DisplayName("consumirEstoque (FIFO)")
    class ConsumirEstoque {

        @Test
        @DisplayName("should consume from oldest lot first (FIFO)")
        void shouldConsumeFromOldestLotFirstFifo() {
            Long ingredienteId = 1L;
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Acucar", "kg",
                    new BigDecimal("100"), new BigDecimal("5.00"));

            // 3 lots ordered by date (oldest first)
            LoteIngrediente lote1 = criarLote(1L, ingrediente, new BigDecimal("30"),
                    new BigDecimal("30"), new BigDecimal("4.00"),
                    LocalDate.of(2025, 1, 1));
            LoteIngrediente lote2 = criarLote(2L, ingrediente, new BigDecimal("40"),
                    new BigDecimal("40"), new BigDecimal("5.00"),
                    LocalDate.of(2025, 2, 1));
            LoteIngrediente lote3 = criarLote(3L, ingrediente, new BigDecimal("30"),
                    new BigDecimal("30"), new BigDecimal("6.00"),
                    LocalDate.of(2025, 3, 1));

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(loteIngredienteRepository.findAllByIngredienteIdOrderByDataCompraAsc(ingredienteId))
                    .thenReturn(List.of(lote1, lote2, lote3));
            when(loteIngredienteRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Consume 20 -- should take all from oldest lot1
            ingredienteService.consumirEstoque(ingredienteId, new BigDecimal("20"));

            // Lote1 should be reduced from 30 to 10
            assertEquals(0, new BigDecimal("10").compareTo(lote1.getQuantidadeAtual()),
                    "Oldest lot should be consumed first, 30 - 20 = 10");
            // Lote2 and Lote3 should remain untouched
            assertEquals(0, new BigDecimal("40").compareTo(lote2.getQuantidadeAtual()),
                    "Second lot should remain untouched");
            assertEquals(0, new BigDecimal("30").compareTo(lote3.getQuantidadeAtual()),
                    "Third lot should remain untouched");

            // Verify estoque was updated
            verify(ingredienteRepository).save(ingredienteCaptor.capture());
            assertEquals(0, new BigDecimal("80").compareTo(ingredienteCaptor.getValue().getEstoqueAtual()),
                    "Total stock should be 100 - 20 = 80");
        }

        @Test
        @DisplayName("should span multiple lots when first lot is insufficient")
        void shouldSpanMultipleLotsWhenFirstLotIsInsufficient() {
            Long ingredienteId = 1L;
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Morango", "kg",
                    new BigDecimal("100"), new BigDecimal("10.00"));

            // 3 lots ordered by date (oldest first)
            LoteIngrediente lote1 = criarLote(1L, ingrediente, new BigDecimal("20"),
                    new BigDecimal("20"), new BigDecimal("8.00"),
                    LocalDate.of(2025, 1, 10));
            LoteIngrediente lote2 = criarLote(2L, ingrediente, new BigDecimal("30"),
                    new BigDecimal("30"), new BigDecimal("10.00"),
                    LocalDate.of(2025, 2, 15));
            LoteIngrediente lote3 = criarLote(3L, ingrediente, new BigDecimal("50"),
                    new BigDecimal("50"), new BigDecimal("12.00"),
                    LocalDate.of(2025, 3, 20));

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(loteIngredienteRepository.findAllByIngredienteIdOrderByDataCompraAsc(ingredienteId))
                    .thenReturn(List.of(lote1, lote2, lote3));
            when(loteIngredienteRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Consume 45: 20 from lote1 + 25 from lote2
            ingredienteService.consumirEstoque(ingredienteId, new BigDecimal("45"));

            // Lote1 fully consumed: 20 - 20 = 0
            assertEquals(0, BigDecimal.ZERO.compareTo(lote1.getQuantidadeAtual()),
                    "First lot should be fully consumed (0)");
            // Lote2 partially consumed: 30 - 25 = 5
            assertEquals(0, new BigDecimal("5").compareTo(lote2.getQuantidadeAtual()),
                    "Second lot should have 30 - 25 = 5 remaining");
            // Lote3 untouched: 50
            assertEquals(0, new BigDecimal("50").compareTo(lote3.getQuantidadeAtual()),
                    "Third lot should remain untouched at 50");

            // Verify estoque updated: 100 - 45 = 55
            verify(ingredienteRepository).save(ingredienteCaptor.capture());
            assertEquals(0, new BigDecimal("55").compareTo(ingredienteCaptor.getValue().getEstoqueAtual()));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when quantidade <= 0")
        void shouldThrowIllegalArgumentExceptionWhenQuantidadeIsZeroOrNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.consumirEstoque(1L, BigDecimal.ZERO)
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.consumirEstoque(1L, new BigDecimal("-5"))
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.consumirEstoque(1L, null)
            );

            verify(ingredienteRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("should throw IllegalStateException when stock is insufficient")
        void shouldThrowIllegalStateExceptionWhenStockIsInsufficient() {
            Long ingredienteId = 1L;
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Acucar", "kg",
                    new BigDecimal("5"), new BigDecimal("3.00"));

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> ingredienteService.consumirEstoque(ingredienteId, new BigDecimal("10"))
            );

            assertTrue(exception.getMessage().contains("Estoque insuficiente"));
            verify(loteIngredienteRepository, never()).findAllByIngredienteIdOrderByDataCompraAsc(anyLong());
            verify(loteIngredienteRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when ingrediente not found")
        void shouldThrowResourceNotFoundExceptionWhenIngredienteNotFound() {
            Long ingredienteId = 999L;
            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> ingredienteService.consumirEstoque(ingredienteId, new BigDecimal("5"))
            );

            verify(loteIngredienteRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should set lot quantidadeAtual to zero when fully consumed")
        void shouldSetLotQuantidadeAtualToZeroWhenFullyConsumed() {
            Long ingredienteId = 1L;
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Corante", "ml",
                    new BigDecimal("25"), new BigDecimal("15.00"));

            LoteIngrediente lote1 = criarLote(1L, ingrediente, new BigDecimal("10"),
                    new BigDecimal("10"), new BigDecimal("15.00"),
                    LocalDate.of(2025, 1, 1));
            LoteIngrediente lote2 = criarLote(2L, ingrediente, new BigDecimal("15"),
                    new BigDecimal("15"), new BigDecimal("16.00"),
                    LocalDate.of(2025, 2, 1));

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(loteIngredienteRepository.findAllByIngredienteIdOrderByDataCompraAsc(ingredienteId))
                    .thenReturn(List.of(lote1, lote2));
            when(loteIngredienteRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Consume exactly 10 to fully exhaust lote1
            ingredienteService.consumirEstoque(ingredienteId, new BigDecimal("10"));

            assertEquals(0, BigDecimal.ZERO.compareTo(lote1.getQuantidadeAtual()),
                    "Fully consumed lot should have quantidadeAtual = 0");
            assertEquals(0, new BigDecimal("15").compareTo(lote2.getQuantidadeAtual()),
                    "Second lot should remain untouched");
        }

        @Test
        @DisplayName("should batch save all modified lots (verify saveAll is called, not save)")
        void shouldBatchSaveAllModifiedLots() {
            Long ingredienteId = 1L;
            Ingrediente ingrediente = criarIngrediente(ingredienteId, "Essencia", "ml",
                    new BigDecimal("60"), new BigDecimal("20.00"));

            LoteIngrediente lote1 = criarLote(1L, ingrediente, new BigDecimal("15"),
                    new BigDecimal("15"), new BigDecimal("18.00"),
                    LocalDate.of(2025, 1, 1));
            LoteIngrediente lote2 = criarLote(2L, ingrediente, new BigDecimal("20"),
                    new BigDecimal("20"), new BigDecimal("20.00"),
                    LocalDate.of(2025, 2, 1));
            LoteIngrediente lote3 = criarLote(3L, ingrediente, new BigDecimal("25"),
                    new BigDecimal("25"), new BigDecimal("22.00"),
                    LocalDate.of(2025, 3, 1));

            when(ingredienteRepository.findById(ingredienteId)).thenReturn(Optional.of(ingrediente));
            when(loteIngredienteRepository.findAllByIngredienteIdOrderByDataCompraAsc(ingredienteId))
                    .thenReturn(List.of(lote1, lote2, lote3));
            when(loteIngredienteRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Consume 30: exhausts lote1 (15) + takes 15 from lote2 (20 - 15 = 5)
            ingredienteService.consumirEstoque(ingredienteId, new BigDecimal("30"));

            // Verify saveAll was called (not individual save calls)
            verify(loteIngredienteRepository, times(1)).saveAll(lotesListCaptor.capture());
            verify(loteIngredienteRepository, never()).save(any(LoteIngrediente.class));

            List<LoteIngrediente> savedLotes = lotesListCaptor.getValue();
            // Both lote1 and lote2 were modified (the loop adds them to lotesModificados)
            assertEquals(2, savedLotes.size(), "Two lots should be batch-saved");
            assertTrue(savedLotes.contains(lote1));
            assertTrue(savedLotes.contains(lote2));
        }
    }

    // =========================================================================
    // atualizarEstoque
    // =========================================================================
    @Nested
    @DisplayName("atualizarEstoque")
    class AtualizarEstoque {

        @Test
        @DisplayName("should add to estoque with positive quantity")
        void shouldAddToEstoqueWithPositiveQuantity() {
            Long id = 1L;
            Ingrediente ingrediente = criarIngrediente(id, "Acucar", "kg",
                    new BigDecimal("50"), new BigDecimal("3.00"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(ingrediente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Ingrediente resultado = ingredienteService.atualizarEstoque(id, new BigDecimal("20"));

            assertEquals(0, new BigDecimal("70").compareTo(resultado.getEstoqueAtual()),
                    "Stock should be 50 + 20 = 70");
        }

        @Test
        @DisplayName("should subtract from estoque with negative quantity")
        void shouldSubtractFromEstoqueWithNegativeQuantity() {
            Long id = 1L;
            Ingrediente ingrediente = criarIngrediente(id, "Acucar", "kg",
                    new BigDecimal("50"), new BigDecimal("3.00"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(ingrediente));
            when(ingredienteRepository.save(any(Ingrediente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Ingrediente resultado = ingredienteService.atualizarEstoque(id, new BigDecimal("-20"));

            assertEquals(0, new BigDecimal("30").compareTo(resultado.getEstoqueAtual()),
                    "Stock should be 50 - 20 = 30");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when result would be negative")
        void shouldThrowIllegalArgumentExceptionWhenResultWouldBeNegative() {
            Long id = 1L;
            Ingrediente ingrediente = criarIngrediente(id, "Acucar", "kg",
                    new BigDecimal("10"), new BigDecimal("3.00"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(ingrediente));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ingredienteService.atualizarEstoque(id, new BigDecimal("-20"))
            );

            assertTrue(exception.getMessage().contains("insuficiente"));
            verify(ingredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            Long id = 999L;
            when(ingredienteRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> ingredienteService.atualizarEstoque(id, BigDecimal.TEN)
            );

            verify(ingredienteRepository, never()).save(any());
        }
    }

    // =========================================================================
    // listarTodos
    // =========================================================================
    @Nested
    @DisplayName("listarTodos")
    class ListarTodos {

        @Test
        @DisplayName("should return all ingredientes")
        void shouldReturnAllIngredientes() {
            Ingrediente ing1 = criarIngrediente(1L, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.00"));
            Ingrediente ing2 = criarIngrediente(2L, "Leite", "litro",
                    new BigDecimal("20"), new BigDecimal("4.50"));
            Ingrediente ing3 = criarIngrediente(3L, "Morango", "kg",
                    new BigDecimal("5"), new BigDecimal("15.00"));

            when(ingredienteRepository.findAll()).thenReturn(List.of(ing1, ing2, ing3));

            List<Ingrediente> resultado = ingredienteService.listarTodos();

            assertNotNull(resultado);
            assertEquals(3, resultado.size());
            assertEquals("Acucar", resultado.get(0).getNome());
            assertEquals("Leite", resultado.get(1).getNome());
            assertEquals("Morango", resultado.get(2).getNome());
            verify(ingredienteRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("should return empty list when no ingredientes exist")
        void shouldReturnEmptyListWhenNoIngredientesExist() {
            when(ingredienteRepository.findAll()).thenReturn(Collections.emptyList());

            List<Ingrediente> resultado = ingredienteService.listarTodos();

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
        }
    }

    // =========================================================================
    // buscarPorId
    // =========================================================================
    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("should return Optional with ingrediente when found")
        void shouldReturnOptionalWithIngredienteWhenFound() {
            Long id = 1L;
            Ingrediente ingrediente = criarIngrediente(id, "Acucar", "kg",
                    BigDecimal.TEN, new BigDecimal("3.00"));

            when(ingredienteRepository.findById(id)).thenReturn(Optional.of(ingrediente));

            Optional<Ingrediente> resultado = ingredienteService.buscarPorId(id);

            assertTrue(resultado.isPresent());
            assertEquals("Acucar", resultado.get().getNome());
            assertEquals(id, resultado.get().getId());
            verify(ingredienteRepository, times(1)).findById(id);
        }

        @Test
        @DisplayName("should return empty Optional when not found")
        void shouldReturnEmptyOptionalWhenNotFound() {
            Long id = 999L;
            when(ingredienteRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Ingrediente> resultado = ingredienteService.buscarPorId(id);

            assertFalse(resultado.isPresent());
            verify(ingredienteRepository, times(1)).findById(id);
        }
    }
}
