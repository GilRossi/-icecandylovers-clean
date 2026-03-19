package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.ProdutoDTO;
import com.icecandylovers.dtos.VendaDTO;
import com.icecandylovers.entities.*;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.services.ProdutoService;
import com.icecandylovers.services.VendaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VendaService vendaService;

    @MockitoBean
    private ProdutoService produtoService;

    private Venda criarVendaComItem() {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setSabor("Chocolate");

        VendaItem item = new VendaItem();
        item.setProduto(produto);
        item.setQuantidade(5);
        item.setValorUnitario(BigDecimal.TEN);

        Venda venda = new Venda();
        venda.setId(1L);
        venda.setDataVenda(LocalDateTime.of(2026, 3, 19, 10, 0));
        venda.setVendido(Vendido.PRAIA);
        venda.setValorUnitarioVenda(BigDecimal.TEN);
        venda.setFormaPagamento("PIX");
        venda.setTotal(BigDecimal.valueOf(50));
        List<VendaItem> itens = new ArrayList<>();
        itens.add(item);
        venda.setItens(itens);
        return venda;
    }

    private VendaDTO criarVendaDTO() {
        return new VendaDTO(1L, 5, Vendido.PRAIA, BigDecimal.TEN, "PIX", LocalDateTime.of(2026, 3, 19, 10, 0));
    }

    // View rendering tests skipped - Thymeleaf templates use _csrf.token
    // which is null when security filters are disabled. Covered by E2E tests.

    @Test
    void registrarVenda_deveRetornarOk() throws Exception {
        Venda venda = criarVendaComItem();
        when(vendaService.registrarVenda(any())).thenReturn(venda);
        VendaDTO dto = criarVendaDTO();

        mockMvc.perform(post("/vendas/nova")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Venda registrada com sucesso!"));
    }

    @Test
    void registrarVenda_deveRetornar400_quandoErroNegocio() throws Exception {
        when(vendaService.registrarVenda(any())).thenThrow(new IllegalArgumentException("Produto invalido"));
        VendaDTO dto = criarVendaDTO();

        mockMvc.perform(post("/vendas/nova")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrarVenda_deveRetornar500_quandoErroInterno() throws Exception {
        when(vendaService.registrarVenda(any())).thenThrow(new RuntimeException("DB error"));
        VendaDTO dto = criarVendaDTO();

        mockMvc.perform(post("/vendas/nova")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // View rendering tests for editar skipped - Thymeleaf templates use _csrf.token

    @Test
    void atualizarVenda_deveRetornarOk() throws Exception {
        Venda venda = criarVendaComItem();
        when(vendaService.atualizarVenda(eq(1L), any())).thenReturn(venda);
        VendaDTO dto = criarVendaDTO();

        mockMvc.perform(put("/vendas/editar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void atualizarVenda_deveRetornar400_quandoErroNegocio() throws Exception {
        when(vendaService.atualizarVenda(eq(1L), any())).thenThrow(new ResourceNotFoundException("Nao encontrada"));
        VendaDTO dto = criarVendaDTO();

        mockMvc.perform(put("/vendas/editar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calcularVendasHoje_deveRetornarTotal() throws Exception {
        when(vendaService.calcularVendasHoje()).thenReturn(BigDecimal.valueOf(500));
        mockMvc.perform(get("/vendas/total-hoje"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVendasHoje").value(500));
    }

    @Test
    void listarVendas_deveRetornarLista() throws Exception {
        Venda venda = criarVendaComItem();
        when(vendaService.listarVendas()).thenReturn(List.of(venda));

        mockMvc.perform(get("/vendas/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].produtoSabor").value("Chocolate"))
                .andExpect(jsonPath("$[0].quantidade").value(5));
    }

    @Test
    void listarVendas_deveRetornar500_quandoErro() throws Exception {
        when(vendaService.listarVendas()).thenThrow(new RuntimeException("DB error"));
        mockMvc.perform(get("/vendas/listar"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deletarVenda_deveRetornarOk() throws Exception {
        doNothing().when(vendaService).deletarVenda(1L);
        mockMvc.perform(delete("/vendas/deletar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deletarVenda_deveRetornar404_quandoNaoEncontrada() throws Exception {
        doThrow(new ResourceNotFoundException("Nao encontrada")).when(vendaService).deletarVenda(999L);
        mockMvc.perform(delete("/vendas/deletar/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deletarVenda_deveRetornar500_quandoErroInterno() throws Exception {
        doThrow(new RuntimeException("DB error")).when(vendaService).deletarVenda(1L);
        mockMvc.perform(delete("/vendas/deletar/1"))
                .andExpect(status().isInternalServerError());
    }
}
