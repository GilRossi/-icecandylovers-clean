package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.PromocaoDTO;
import com.icecandylovers.entities.TipoDesconto;
import com.icecandylovers.services.PromocaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PromocaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PromocaoController Unit Tests")
class PromocaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PromocaoService promocaoService;

    private PromocaoDTO buildDto(Long id, String nome) {
        return new PromocaoDTO(
                id,
                nome,
                "Descrição teste",
                TipoDesconto.PERCENTUAL,
                new BigDecimal("10.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                true,
                null
        );
    }

    @Nested
    @DisplayName("GET /promocoes/listar")
    class Listar {

        @Test
        @DisplayName("deve retornar lista de promoções com status 200")
        void deve_retornar_lista_promocoes() throws Exception {
            // given
            List<PromocaoDTO> lista = List.of(buildDto(1L, "Promo Verão"), buildDto(2L, "Promo Inverno"));
            when(promocaoService.listarTodas()).thenReturn(lista);

            // when / then
            mockMvc.perform(get("/promocoes/listar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].nome").value("Promo Verão"))
                    .andExpect(jsonPath("$[1].nome").value("Promo Inverno"));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há promoções")
        void deve_retornar_lista_vazia() throws Exception {
            // given
            when(promocaoService.listarTodas()).thenReturn(Collections.emptyList());

            // when / then
            mockMvc.perform(get("/promocoes/listar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /promocoes/salvar")
    class Criar {

        @Test
        @DisplayName("deve criar promoção e retornar id com status 200")
        void deve_criar_promocao_e_retornar_id() throws Exception {
            // given
            PromocaoDTO dto = buildDto(null, "Nova Promo");
            PromocaoDTO criada = buildDto(5L, "Nova Promo");
            when(promocaoService.criarPromocao(any(PromocaoDTO.class))).thenReturn(criada);

            // when / then
            mockMvc.perform(post("/promocoes/salvar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.id").value(5));
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void deve_retornar_400_quando_nome_em_branco() throws Exception {
            // given - dto com nome em branco falha @NotBlank
            PromocaoDTO dtoInvalido = new PromocaoDTO(
                    null, "", null,
                    TipoDesconto.PERCENTUAL, new BigDecimal("10.00"),
                    LocalDate.now(), LocalDate.now().plusDays(7),
                    true, null
            );

            // when / then
            mockMvc.perform(post("/promocoes/salvar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoInvalido)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /promocoes/deletar/{id}")
    class Deletar {

        @Test
        @DisplayName("deve deletar promoção e retornar success true")
        void deve_deletar_promocao() throws Exception {
            // given
            doNothing().when(promocaoService).deletarPromocao(1L);

            // when / then
            mockMvc.perform(delete("/promocoes/deletar/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(promocaoService, times(1)).deletarPromocao(1L);
        }
    }

    @Nested
    @DisplayName("GET /promocoes/produto/{produtoId}")
    class PromocoesPorProduto {

        @Test
        @DisplayName("deve retornar promoções ativas para o produto")
        void deve_retornar_promocoes_para_produto() throws Exception {
            // given
            List<PromocaoDTO> lista = List.of(buildDto(1L, "Promo Morango"));
            when(promocaoService.buscarPromocoesParaProduto(1L)).thenReturn(lista);

            // when / then
            mockMvc.perform(get("/promocoes/produto/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nome").value("Promo Morango"));
        }
    }
}
