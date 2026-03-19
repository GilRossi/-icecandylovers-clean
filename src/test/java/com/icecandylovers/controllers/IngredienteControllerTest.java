package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.*;
import com.icecandylovers.entities.Ingrediente;
import com.icecandylovers.entities.LoteIngrediente;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.services.IngredienteService;
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

@WebMvcTest(IngredienteController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class IngredienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngredienteService ingredienteService;

    private Ingrediente criarIngrediente(Long id, String nome) {
        Ingrediente ing = new Ingrediente();
        ing.setId(id);
        ing.setNome(nome);
        ing.setUnidadeMedida("kg");
        ing.setEstoqueAtual(BigDecimal.TEN);
        ing.setCustoPorUnidade(BigDecimal.valueOf(5.00));
        return ing;
    }

    // View rendering tests are skipped because Thymeleaf templates use _csrf.token
    // which is null when security filters are disabled in @WebMvcTest.
    // The page rendering is covered by E2E tests.

    @Test
    void buscarIngrediente_deveRetornarOk_quandoEncontrado() throws Exception {
        Ingrediente ing = criarIngrediente(1L, "Leite Condensado");
        when(ingredienteService.buscarPorId(1L)).thenReturn(Optional.of(ing));

        mockMvc.perform(get("/ingredientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Leite Condensado"));
    }

    @Test
    void buscarIngrediente_deveRetornar404_quandoNaoEncontrado() throws Exception {
        when(ingredienteService.buscarPorId(999L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/ingredientes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarIngredientes_deveRetornarLista() throws Exception {
        when(ingredienteService.listarTodos()).thenReturn(List.of(criarIngrediente(1L, "Leite"), criarIngrediente(2L, "Acucar")));

        mockMvc.perform(get("/ingredientes/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("Leite"))
                .andExpect(jsonPath("$[1].nome").value("Acucar"));
    }

    @Test
    void deletarIngrediente_deveRetornarOk() throws Exception {
        doNothing().when(ingredienteService).deletar(1L);
        mockMvc.perform(delete("/ingredientes/deletar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ingrediente deletado com sucesso!"));
    }

    @Test
    void editarIngrediente_deveRetornarOk() throws Exception {
        Ingrediente atualizado = criarIngrediente(1L, "Leite Integral");
        when(ingredienteService.editar(eq(1L), eq("Leite Integral"), any(), eq("litro"), any()))
                .thenReturn(atualizado);

        IngredienteEditDTO dto = new IngredienteEditDTO(1L, "Leite Integral", BigDecimal.valueOf(5.00), "litro", BigDecimal.TEN);

        mockMvc.perform(post("/ingredientes/editar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ingrediente atualizado com sucesso!"));
    }

    @Test
    void salvarIngrediente_deveRetornarOk() throws Exception {
        Ingrediente criado = criarIngrediente(1L, "Morango");
        when(ingredienteService.criarComLoteInicial(eq("Morango"), eq("kg"), any(), any()))
                .thenReturn(criado);

        IngredienteDTO ingDto = new IngredienteDTO(null, "Morango", BigDecimal.valueOf(3.00), "kg", BigDecimal.TEN, BigDecimal.ZERO);
        LoteDTO loteDto = new LoteDTO(BigDecimal.TEN, BigDecimal.valueOf(3.00));
        IngredienteRequestDTO request = new IngredienteRequestDTO(ingDto, loteDto);

        mockMvc.perform(post("/ingredientes/salvar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ingrediente salvo com sucesso!"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void adicionarLote_deveRetornarOk() throws Exception {
        LoteIngrediente lote = new LoteIngrediente();
        lote.setId(10L);
        lote.setQuantidade(BigDecimal.valueOf(20));
        Ingrediente ing = criarIngrediente(1L, "Leite");
        ing.setEstoqueAtual(BigDecimal.valueOf(30));
        ing.setCustoPorUnidade(BigDecimal.valueOf(4.50));

        when(ingredienteService.adicionarLote(eq(1L), eq(BigDecimal.valueOf(20)), any())).thenReturn(lote);
        when(ingredienteService.buscarPorId(1L)).thenReturn(Optional.of(ing));

        mockMvc.perform(post("/ingredientes/1/adicionar-lote")
                        .param("quantidade", "20")
                        .param("valorTotal", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lote adicionado com sucesso!"))
                .andExpect(jsonPath("$.loteId").value(10));
    }

    @Test
    void adicionarLote_deveRetornar400_quandoQuantidadeInvalida() throws Exception {
        mockMvc.perform(post("/ingredientes/1/adicionar-lote")
                        .param("quantidade", "0")
                        .param("valorTotal", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consumirEstoque_deveRetornarOk() throws Exception {
        doNothing().when(ingredienteService).consumirEstoque(1L, BigDecimal.valueOf(5));
        mockMvc.perform(post("/ingredientes/1/consumir").param("quantidade", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Estoque consumido com sucesso!"));
    }

    @Test
    void atualizarEstoque_deveRetornarOk() throws Exception {
        Ingrediente atualizado = criarIngrediente(1L, "Leite");
        atualizado.setEstoqueAtual(BigDecimal.valueOf(15));
        when(ingredienteService.atualizarEstoque(1L, BigDecimal.valueOf(5))).thenReturn(atualizado);

        mockMvc.perform(patch("/ingredientes/1/atualizar-estoque").param("quantidade", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Estoque atualizado com sucesso!"))
                .andExpect(jsonPath("$.estoqueAtual").value(15));
    }
}
