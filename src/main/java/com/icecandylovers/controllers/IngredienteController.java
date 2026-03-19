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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @Autowired
    public IngredienteController(IngredienteService ingredienteService) {
        this.ingredienteService = ingredienteService;
    }

    // Renderiza a página de ingredientes (GET /ingredientes)
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
        try {
            logger.info("Buscando todos os ingredientes...");
            List<Ingrediente> ingredientes = ingredienteService.listarTodos();
            logger.debug("Número de ingredientes encontrados: {}", ingredientes.size());
            List<IngredienteResponseDTO> response = ingredientes.stream()
                    .map(IngredienteResponseDTO::new)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao listar ingredientes:", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Deleta um ingrediente por ID (endpoint REST)
    @DeleteMapping("/deletar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletarIngrediente(@PathVariable Long id) {
        logger.info("Requisição para deletar ingrediente ID: {}", id);
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Ingrediente> optionalIngrediente = ingredienteService.buscarPorId(id);
            if (optionalIngrediente.isEmpty()) {
                logger.warn("Ingrediente não encontrado para deleção: {}", id);
                response.put("error", "Ingrediente não encontrado com ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            ingredienteService.deletar(id);
            response.put("message", "Ingrediente deletado com sucesso!");
            logger.debug("Ingrediente ID {} deletado com sucesso", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao deletar ingrediente ID {}: {}", id, e.getMessage(), e);
            response.put("error", "Erro ao deletar ingrediente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Edita um ingrediente existente (endpoint REST)
    @PostMapping("/editar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editarIngrediente(@Valid @RequestBody IngredienteEditDTO request) {
        logger.info("Requisição para editar ingrediente ID: {}", request.id());
        Map<String, Object> response = new HashMap<>();
        try {
            Ingrediente atualizado = ingredienteService.editar(
                    request.id(),
                    request.nome(),
                    request.custoPorUnidade(),
                    request.unidadeMedida(),
                    request.estoqueAtual()
            );
            response.put("message", "Ingrediente atualizado com sucesso!");
            response.put("ingrediente", new IngredienteResponseDTO(atualizado));
            logger.debug("Ingrediente ID {} atualizado com sucesso", request.id());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação ao editar ingrediente ID {}: {}", request.id(), e.getMessage());
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Erro ao editar ingrediente ID {}: {}", request.id(), e.getMessage(), e);
            response.put("error", "Erro ao editar ingrediente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Salva um novo ingrediente com lote inicial (endpoint REST)
    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarIngrediente(@Valid @RequestBody IngredienteRequestDTO request) {
        logger.info("Requisição para salvar novo ingrediente: {}", request.ingrediente().nome());
        Map<String, Object> response = new HashMap<>();
        try {
            Ingrediente ingrediente = new Ingrediente();
            ingrediente.setNome(request.ingrediente().nome());
            ingrediente.setUnidadeMedida(request.ingrediente().unidadeMedida());
            ingrediente.setEstoqueInicial(request.lote().quantidade());
            ingrediente.setEstoqueAtual(BigDecimal.ZERO);
            ingrediente.setCustoPorUnidade(BigDecimal.ZERO);

            Ingrediente ingredienteSalvo = ingredienteService.salvar(ingrediente);
            LoteIngrediente lote = ingredienteService.adicionarLote(
                    ingredienteSalvo.getId(),
                    request.lote().quantidade(),
                    request.lote().custoPorUnidade()
            );

            // Recarrega o ingrediente atualizado
            Ingrediente atualizado = ingredienteService.buscarPorId(ingredienteSalvo.getId()).get();

            response.put("message", "Ingrediente salvo com sucesso!");
            response.put("id", ingredienteSalvo.getId());
            response.put("loteId", lote.getId());
            response.put("ingrediente", new IngredienteResponseDTO(atualizado)); // Retorna o DTO completo
            logger.debug("Ingrediente '{}' salvo com sucesso, ID: {}", ingredienteSalvo.getNome(), ingredienteSalvo.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao salvar ingrediente: {}", e.getMessage(), e);
            response.put("error", "Erro ao salvar ingrediente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Adiciona um novo lote a um ingrediente existente (endpoint REST)
    @PostMapping("/{id}/adicionar-lote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adicionarLote(
            @PathVariable Long id,
            @RequestParam("quantidade") BigDecimal quantidade,
            @RequestParam("valorTotal") BigDecimal valorTotal) {
        logger.info("Requisição para adicionar lote ao ingrediente ID: {}", id);
        Map<String, Object> response = new HashMap<>();
        try {
            validarParametrosLote(quantidade, valorTotal, id, response);
            if (response.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Optional<Ingrediente> optionalIngrediente = ingredienteService.buscarPorId(id);
            if (optionalIngrediente.isEmpty()) {
                logger.warn("Ingrediente não encontrado para adicionar lote: {}", id);
                response.put("error", "Ingrediente não encontrado!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            BigDecimal custoPorUnidadeLote = valorTotal.divide(quantidade, 2, RoundingMode.HALF_UP);
            LoteIngrediente lote = ingredienteService.adicionarLote(id, quantidade, custoPorUnidadeLote);

            // Recarrega o ingrediente atualizado
            Ingrediente atualizado = ingredienteService.buscarPorId(id).get();

            response.put("message", "Lote adicionado com sucesso!");
            response.put("loteId", lote.getId());
            response.put("ingrediente", new IngredienteResponseDTO(atualizado));
            response.put("estoqueAtual", atualizado.getEstoqueAtual());
            response.put("custoPorUnidade", atualizado.getCustoPorUnidade());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação ao adicionar lote ao ingrediente ID {}: {}", id, e.getMessage());
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (ArithmeticException e) {
            logger.warn("Erro aritmético ao adicionar lote ao ingrediente ID {}: {}", id, e.getMessage());
            response.put("error", "Erro ao calcular custo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Erro ao adicionar lote ao ingrediente ID {}: {}", id, e.getMessage(), e);
            response.put("error", "Erro ao adicionar lote: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Consome estoque de um ingrediente (endpoint REST)
    @PostMapping("/{id}/consumir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consumirEstoque(
            @PathVariable Long id,
            @RequestParam BigDecimal quantidade) {
        logger.info("Requisição para consumir estoque do ingrediente ID: {}", id);
        Map<String, Object> response = new HashMap<>();
        try {
            ingredienteService.consumirEstoque(id, quantidade);
            response.put("message", "Estoque consumido com sucesso!");
            logger.debug("Estoque consumido com sucesso para ingrediente ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Erro ao consumir estoque do ingrediente ID {}: {}", id, e.getMessage());
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Erro ao consumir estoque do ingrediente ID {}: {}", id, e.getMessage(), e);
            response.put("error", "Erro ao consumir estoque: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Atualiza o estoque de um ingrediente (endpoint REST)
    @PatchMapping("/{id}/atualizar-estoque")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizarEstoque(
            @PathVariable Long id,
            @RequestParam("quantidade") BigDecimal quantidade) {
        logger.info("Requisição para atualizar estoque do ingrediente ID: {}", id);
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Ingrediente> optionalIngrediente = ingredienteService.buscarPorId(id);
            if (optionalIngrediente.isEmpty()) {
                logger.warn("Ingrediente não encontrado para atualização de estoque: {}", id);
                response.put("error", "Ingrediente não encontrado!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Ingrediente ingrediente = optionalIngrediente.get();
            BigDecimal novoEstoque = ingrediente.getEstoqueAtual().add(quantidade);
            if (novoEstoque.compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Estoque insuficiente para ingrediente ID {}: atual={}, solicitado={}", id, ingrediente.getEstoqueAtual(), quantidade);
                response.put("error", "Estoque insuficiente!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            ingrediente.setEstoqueAtual(novoEstoque);
            ingredienteService.salvar(ingrediente);
            response.put("message", "Estoque atualizado com sucesso!");
            response.put("estoqueAtual", novoEstoque);
            logger.debug("Estoque atualizado para ingrediente ID {}: novo estoque={}", id, novoEstoque);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao atualizar estoque do ingrediente ID {}: {}", id, e.getMessage(), e);
            response.put("error", "Erro ao atualizar estoque: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Métodos auxiliares privados para melhorar legibilidade e reutilização
    private void validarParametrosLote(BigDecimal quantidade, BigDecimal valorTotal, Long id, Map<String, Object> response) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Quantidade inválida para lote do ingrediente ID {}: {}", id, quantidade);
            response.put("error", "Quantidade deve ser maior que zero!");
        } else if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Valor total inválido para lote do ingrediente ID {}: {}", id, valorTotal);
            response.put("error", "Valor total não pode ser negativo!");
        }
    }

}
