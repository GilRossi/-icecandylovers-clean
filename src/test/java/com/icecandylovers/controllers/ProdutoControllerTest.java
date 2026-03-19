package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.ProdutoDTO;
import com.icecandylovers.entities.CategoriaProduto;
import com.icecandylovers.exceptions.DuplicateResourceException;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.services.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProdutoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProdutoService produtoService;

    private ProdutoDTO criarProdutoDTO(Long id, String sabor) {
        return new ProdutoDTO(id, sabor, 100, 50, 10.0, 0.1, CategoriaProduto.GELADINHO_GOURMET, List.of());
    }

    @Test
    void listarProdutos_deveRetornarListaOk() throws Exception {
        when(produtoService.listarProdutos()).thenReturn(List.of(criarProdutoDTO(1L, "Chocolate")));
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sabor").value("Chocolate"));
    }

    @Test
    void listarProdutos_deveRetornar500_quandoErro() throws Exception {
        when(produtoService.listarProdutos()).thenThrow(new RuntimeException("DB error"));
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void buscarProdutoPorId_deveRetornarProduto_quandoEncontrado() throws Exception {
        when(produtoService.buscarProdutoPorId(1L)).thenReturn(Optional.of(criarProdutoDTO(1L, "Morango")));
        mockMvc.perform(get("/api/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sabor").value("Morango"));
    }

    @Test
    void buscarProdutoPorId_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(produtoService.buscarProdutoPorId(999L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/produtos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void criarProduto_deveRetornar201_quandoSucesso() throws Exception {
        ProdutoDTO dto = criarProdutoDTO(null, "Limao");
        ProdutoDTO criado = criarProdutoDTO(1L, "Limao");
        when(produtoService.criarProduto(any())).thenReturn(criado);

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sabor").value("Limao"));
    }

    @Test
    void criarProduto_deveRetornar409_quandoDuplicado() throws Exception {
        ProdutoDTO dto = criarProdutoDTO(null, "Limao");
        when(produtoService.criarProduto(any())).thenThrow(new DuplicateResourceException("Duplicado"));

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void criarProduto_deveRetornar400_quandoRecursoNaoEncontrado() throws Exception {
        ProdutoDTO dto = criarProdutoDTO(null, "Limao");
        when(produtoService.criarProduto(any())).thenThrow(new ResourceNotFoundException("Ingrediente nao encontrado"));

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarProduto_deveRetornarOk() throws Exception {
        ProdutoDTO dto = criarProdutoDTO(1L, "Chocolate Belga");
        when(produtoService.atualizarProduto(any())).thenReturn(dto);

        mockMvc.perform(put("/api/produtos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sabor").value("Chocolate Belga"));
    }

    @Test
    void atualizarProduto_deveRetornar400_quandoIdsDiferentes() throws Exception {
        ProdutoDTO dto = criarProdutoDTO(2L, "Chocolate");
        mockMvc.perform(put("/api/produtos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarProduto_deveRetornar404_quandoNaoEncontrado() throws Exception {
        ProdutoDTO dto = criarProdutoDTO(999L, "Inexistente");
        when(produtoService.atualizarProduto(any())).thenThrow(new ResourceNotFoundException("Nao encontrado"));

        mockMvc.perform(put("/api/produtos/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletarProduto_deveRetornarOk() throws Exception {
        doNothing().when(produtoService).deletarProduto(1L);
        mockMvc.perform(delete("/api/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Produto deletado com sucesso"));
    }

    @Test
    void deletarProduto_deveRetornar409_quandoTemVendas() throws Exception {
        doThrow(new IllegalStateException("Possui vendas")).when(produtoService).deletarProduto(1L);
        mockMvc.perform(delete("/api/produtos/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void deletarProduto_deveRetornar404_quandoNaoEncontrado() throws Exception {
        doThrow(new ResourceNotFoundException("Nao encontrado")).when(produtoService).deletarProduto(999L);
        mockMvc.perform(delete("/api/produtos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void produzirProduto_deveRetornarOk() throws Exception {
        doNothing().when(produtoService).produzirProduto(1L, 10);
        mockMvc.perform(post("/api/produtos/1/produzir").param("quantidade", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Produção realizada com sucesso"));
    }

    @Test
    void produzirProduto_deveRetornar400_quandoQuantidadeZero() throws Exception {
        mockMvc.perform(post("/api/produtos/1/produzir").param("quantidade", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void produzirProduto_deveRetornar404_quandoNaoEncontrado() throws Exception {
        doThrow(new ResourceNotFoundException("Nao encontrado")).when(produtoService).produzirProduto(999L, 5);
        mockMvc.perform(post("/api/produtos/999/produzir").param("quantidade", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void calcularCusto_deveRetornarValor() throws Exception {
        when(produtoService.calcularCusto(1L)).thenReturn(BigDecimal.valueOf(25.50));
        mockMvc.perform(get("/api/produtos/1/custo"))
                .andExpect(status().isOk())
                .andExpect(content().string("25.5"));
    }

    @Test
    void calcularCusto_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(produtoService.calcularCusto(999L)).thenThrow(new ResourceNotFoundException("Nao encontrado"));
        mockMvc.perform(get("/api/produtos/999/custo"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorCategoria_deveRetornarLista() throws Exception {
        when(produtoService.listarProdutosPorCategoria(CategoriaProduto.GELADINHO_GOURMET))
                .thenReturn(List.of(criarProdutoDTO(1L, "Chocolate")));
        mockMvc.perform(get("/api/produtos/categoria/GELADINHO_GOURMET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sabor").value("Chocolate"));
    }

    @Test
    void listarPorCategoria_deveRetornar400_quandoCategoriaInvalida() throws Exception {
        mockMvc.perform(get("/api/produtos/categoria/INVALIDA"))
                .andExpect(status().isBadRequest());
    }

    // View rendering test skipped - Thymeleaf templates use _csrf.token
    // which is null when security filters are disabled. Covered by E2E tests.
}
