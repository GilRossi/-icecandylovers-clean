package com.icecandylovers.services;

import com.icecandylovers.dtos.PromocaoDTO;
import com.icecandylovers.entities.Produto;
import com.icecandylovers.entities.Promocao;
import com.icecandylovers.entities.TipoDesconto;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.repositories.ProdutoRepository;
import com.icecandylovers.repositories.PromocaoRepository;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromocaoService Unit Tests")
class PromocaoServiceTest {

    @Mock
    private PromocaoRepository promocaoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private PromocaoService promocaoService;

    private Promocao promocaoAtiva;
    private PromocaoDTO dtoCriar;

    @BeforeEach
    void setUp() {
        promocaoAtiva = new Promocao();
        promocaoAtiva.setId(1L);
        promocaoAtiva.setNome("Desconto Verão");
        promocaoAtiva.setDescricao("10% off em todos os produtos");
        promocaoAtiva.setTipo(TipoDesconto.PERCENTUAL);
        promocaoAtiva.setValor(new BigDecimal("10.00"));
        promocaoAtiva.setDataInicio(LocalDate.now().minusDays(1));
        promocaoAtiva.setDataFim(LocalDate.now().plusDays(30));
        promocaoAtiva.setAtivo(true);
        promocaoAtiva.setProdutos(Collections.emptyList());

        dtoCriar = new PromocaoDTO(
                null,
                "Promoção Teste",
                "Descrição teste",
                TipoDesconto.PERCENTUAL,
                new BigDecimal("15.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                true,
                null
        );
    }

    @Nested
    @DisplayName("criarPromocao")
    class CriarPromocao {

        @Test
        @DisplayName("deve criar promoção com sucesso - happy path")
        void deve_criar_promocao_com_sucesso() {
            // given
            Promocao salva = new Promocao();
            salva.setId(10L);
            salva.setNome(dtoCriar.nome());
            salva.setTipo(dtoCriar.tipo());
            salva.setValor(dtoCriar.valor());
            salva.setDataInicio(dtoCriar.dataInicio());
            salva.setDataFim(dtoCriar.dataFim());
            salva.setAtivo(true);
            salva.setProdutos(Collections.emptyList());

            when(promocaoRepository.save(any(Promocao.class))).thenReturn(salva);

            // when
            PromocaoDTO resultado = promocaoService.criarPromocao(dtoCriar);

            // then
            assertNotNull(resultado);
            assertEquals(10L, resultado.id());
            assertEquals("Promoção Teste", resultado.nome());
            verify(promocaoRepository, times(1)).save(any(Promocao.class));
        }

        @Test
        @DisplayName("deve resolver produto IDs ao criar promoção")
        void deve_resolver_produto_ids_ao_criar() {
            // given
            Produto produto = new Produto();
            produto.setId(5L);
            produto.setSabor("Morango");

            PromocaoDTO dtoComProduto = new PromocaoDTO(
                    null, "Promo com produto", null,
                    TipoDesconto.FIXO, new BigDecimal("2.00"),
                    LocalDate.now(), LocalDate.now().plusDays(5),
                    true, List.of(5L)
            );

            Promocao salva = new Promocao();
            salva.setId(11L);
            salva.setNome("Promo com produto");
            salva.setTipo(TipoDesconto.FIXO);
            salva.setValor(new BigDecimal("2.00"));
            salva.setDataInicio(dtoComProduto.dataInicio());
            salva.setDataFim(dtoComProduto.dataFim());
            salva.setAtivo(true);
            salva.setProdutos(List.of(produto));

            when(produtoRepository.findById(5L)).thenReturn(Optional.of(produto));
            when(promocaoRepository.save(any(Promocao.class))).thenReturn(salva);

            // when
            PromocaoDTO resultado = promocaoService.criarPromocao(dtoComProduto);

            // then
            assertNotNull(resultado);
            assertEquals(List.of(5L), resultado.produtoIds());
            verify(produtoRepository, times(1)).findById(5L);
        }
    }

    @Nested
    @DisplayName("calcularPrecoComDesconto")
    class CalcularPrecoComDesconto {

        @Test
        @DisplayName("deve aplicar desconto percentual corretamente")
        void deve_aplicar_desconto_percentual() {
            // given - 10% off em R$ 50.00 => R$ 45.00
            Promocao promocao = new Promocao();
            promocao.setTipo(TipoDesconto.PERCENTUAL);
            promocao.setValor(new BigDecimal("10.00"));

            // when
            BigDecimal resultado = promocaoService.calcularPrecoComDesconto(new BigDecimal("50.00"), promocao);

            // then
            assertEquals(new BigDecimal("45.00"), resultado);
        }

        @Test
        @DisplayName("deve aplicar desconto fixo corretamente")
        void deve_aplicar_desconto_fixo() {
            // given - R$ 5.00 off em R$ 20.00 => R$ 15.00
            Promocao promocao = new Promocao();
            promocao.setTipo(TipoDesconto.FIXO);
            promocao.setValor(new BigDecimal("5.00"));

            // when
            BigDecimal resultado = promocaoService.calcularPrecoComDesconto(new BigDecimal("20.00"), promocao);

            // then
            assertEquals(new BigDecimal("15.00"), resultado);
        }

        @Test
        @DisplayName("não deve retornar preço negativo para desconto fixo maior que o preço")
        void nao_deve_retornar_preco_negativo() {
            // given - R$ 30.00 off em R$ 10.00 => deve retornar R$ 0.00
            Promocao promocao = new Promocao();
            promocao.setTipo(TipoDesconto.FIXO);
            promocao.setValor(new BigDecimal("30.00"));

            // when
            BigDecimal resultado = promocaoService.calcularPrecoComDesconto(new BigDecimal("10.00"), promocao);

            // then
            assertEquals(BigDecimal.ZERO, resultado);
        }
    }

    @Nested
    @DisplayName("buscarPromocoesParaProduto")
    class BuscarPromocoesParaProduto {

        @Test
        @DisplayName("deve retornar promoções ativas para o produto")
        void deve_retornar_promocoes_ativas() {
            // given
            when(promocaoRepository.findActiveByProdutoId(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of(promocaoAtiva));

            // when
            List<PromocaoDTO> resultado = promocaoService.buscarPromocoesParaProduto(1L);

            // then
            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            assertEquals("Desconto Verão", resultado.get(0).nome());
            verify(promocaoRepository, times(1)).findActiveByProdutoId(eq(1L), any(LocalDate.class));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há promoções ativas")
        void deve_retornar_lista_vazia_sem_promocoes() {
            // given
            when(promocaoRepository.findActiveByProdutoId(eq(99L), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // when
            List<PromocaoDTO> resultado = promocaoService.buscarPromocoesParaProduto(99L);

            // then
            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
        }
    }

    @Nested
    @DisplayName("ativarDesativar")
    class AtivarDesativar {

        @Test
        @DisplayName("deve ativar promoção inativa")
        void deve_ativar_promocao_inativa() {
            // given
            promocaoAtiva.setAtivo(false);
            when(promocaoRepository.findById(1L)).thenReturn(Optional.of(promocaoAtiva));
            when(promocaoRepository.save(any(Promocao.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            PromocaoDTO resultado = promocaoService.ativarDesativar(1L, true);

            // then
            assertTrue(resultado.ativo());
            verify(promocaoRepository, times(1)).save(any(Promocao.class));
        }

        @Test
        @DisplayName("deve desativar promoção ativa")
        void deve_desativar_promocao_ativa() {
            // given
            when(promocaoRepository.findById(1L)).thenReturn(Optional.of(promocaoAtiva));
            when(promocaoRepository.save(any(Promocao.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            PromocaoDTO resultado = promocaoService.ativarDesativar(1L, false);

            // then
            assertFalse(resultado.ativo());
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando promoção não existe")
        void deve_lancar_excecao_quando_nao_encontrada() {
            // given
            when(promocaoRepository.findById(999L)).thenReturn(Optional.empty());

            // when / then
            assertThrows(ResourceNotFoundException.class,
                    () -> promocaoService.ativarDesativar(999L, false));
            verify(promocaoRepository, never()).save(any());
        }
    }
}
