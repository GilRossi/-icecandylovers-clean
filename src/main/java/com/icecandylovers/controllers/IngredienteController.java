package com.icecandylovers.controllers;

import com.icecandylovers.dtos.IngredienteEditDTO;
import com.icecandylovers.dtos.IngredienteRequestDTO;
import com.icecandylovers.dtos.IngredienteResponseDTO;
import com.icecandylovers.entities.Ingrediente;
import com.icecandylovers.entities.LoteIngrediente;
import com.icecandylovers.services.IngredienteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/ingredientes")
public class IngredienteController {

    private static final Logger logger = LoggerFactory.getLogger(IngredienteController.class);

    private final IngredienteService ingredienteService;

    public IngredienteController(IngredienteService ingredienteService) {
        this.ingredienteService = ingredienteService;
    }

    // Renderiza a pagina de ingredientes (GET /ingredientes)
    @GetMapping
    public String mostrarPaginaIngredientes() {
        return "ingredientes";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<IngredienteResponseDTO> buscarIngrediente(@PathVariable Long id) {
        Optional<Ingrediente> ingrediente = ingredienteService.buscarPorId(id);
        return ingrediente.map(value -> ResponseEntity.ok(new IngredienteResponseDTO(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lista todos os ingredientes (endpoint REST)
    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<List<IngredienteResponseDTO>> listarIngredientes() {
        logger.info("Buscando todos os ingredientes...");
        List<Ingrediente> ingredientes = ingredienteService.listarTodos();
        logger.debug("Número de ingredientes encontrados: {}", ingredientes.size());
        List<IngredienteResponseDTO> response = ingredientes.stream()
                .map(IngredienteResponseDTO::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    // Deleta um ingrediente por ID (endpoint REST)
    @DeleteMapping("/deletar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletarIngrediente(@PathVariable Long id) {
        logger.info("Requisição para deletar ingrediente ID: {}", id);
        ingredienteService.deletar(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ingrediente deletado com sucesso!");
        logger.debug("Ingrediente ID {} deletado com sucesso", id);
        return ResponseEntity.ok(response);
    }

    // Edita um ingrediente existente (endpoint REST)
    @PostMapping("/editar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editarIngrediente(@Valid @RequestBody IngredienteEditDTO request) {
        logger.info("Requisição para editar ingrediente ID: {}", request.id());
        Ingrediente atualizado = ingredienteService.editar(
                request.id(),
                request.nome(),
                request.custoPorUnidade(),
                request.unidadeMedida(),
                request.estoqueAtual()
        );
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ingrediente atualizado com sucesso!");
        response.put("ingrediente", new IngredienteResponseDTO(atualizado));
        logger.debug("Ingrediente ID {} atualizado com sucesso", request.id());
        return ResponseEntity.ok(response);
    }

    // Salva um novo ingrediente com lote inicial (endpoint REST)
    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarIngrediente(@Valid @RequestBody IngredienteRequestDTO request) {
        logger.info("Requisição para salvar novo ingrediente: {}", request.ingrediente().nome());
        Ingrediente atualizado = ingredienteService.criarComLoteInicial(
                request.ingrediente().nome(),
                request.ingrediente().unidadeMedida(),
                request.lote().quantidade(),
                request.lote().custoPorUnidade()
        );
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ingrediente salvo com sucesso!");
        response.put("id", atualizado.getId());
        response.put("ingrediente", new IngredienteResponseDTO(atualizado));
        logger.debug("Ingrediente '{}' salvo com sucesso, ID: {}", atualizado.getNome(), atualizado.getId());
        return ResponseEntity.ok(response);
    }

    // Adiciona um novo lote a um ingrediente existente (endpoint REST)
    @PostMapping("/{id}/adicionar-lote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adicionarLote(
            @PathVariable Long id,
            @RequestParam("quantidade") BigDecimal quantidade,
            @RequestParam("valorTotal") BigDecimal valorTotal) {
        logger.info("Requisição para adicionar lote ao ingrediente ID: {}", id);

        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero!");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total não pode ser negativo!");
        }

        BigDecimal custoPorUnidadeLote = valorTotal.divide(quantidade, 2, RoundingMode.HALF_UP);
        LoteIngrediente lote = ingredienteService.adicionarLote(id, quantidade, custoPorUnidadeLote);

        // Recarrega o ingrediente atualizado
        Ingrediente atualizado = ingredienteService.buscarPorId(id)
                .orElseThrow(); // Should never happen since lote was just added

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lote adicionado com sucesso!");
        response.put("loteId", lote.getId());
        response.put("ingrediente", new IngredienteResponseDTO(atualizado));
        response.put("estoqueAtual", atualizado.getEstoqueAtual());
        response.put("custoPorUnidade", atualizado.getCustoPorUnidade());
        return ResponseEntity.ok(response);
    }

    // Consome estoque de um ingrediente (endpoint REST)
    @PostMapping("/{id}/consumir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consumirEstoque(
            @PathVariable Long id,
            @RequestParam BigDecimal quantidade) {
        logger.info("Requisição para consumir estoque do ingrediente ID: {}", id);
        ingredienteService.consumirEstoque(id, quantidade);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Estoque consumido com sucesso!");
        logger.debug("Estoque consumido com sucesso para ingrediente ID: {}", id);
        return ResponseEntity.ok(response);
    }

    // Atualiza o estoque de um ingrediente (endpoint REST)
    @PatchMapping("/{id}/atualizar-estoque")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizarEstoque(
            @PathVariable Long id,
            @RequestParam("quantidade") BigDecimal quantidade) {
        logger.info("Requisição para atualizar estoque do ingrediente ID: {}", id);
        Ingrediente atualizado = ingredienteService.atualizarEstoque(id, quantidade);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Estoque atualizado com sucesso!");
        response.put("estoqueAtual", atualizado.getEstoqueAtual());
        logger.debug("Estoque atualizado para ingrediente ID {}: novo estoque={}", id, atualizado.getEstoqueAtual());
        return ResponseEntity.ok(response);
    }
}
