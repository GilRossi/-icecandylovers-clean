package com.icecandylovers.controllers;

import com.icecandylovers.dtos.VendaDTO;
import com.icecandylovers.entities.Venda;
import com.icecandylovers.entities.VendaItem;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import com.icecandylovers.services.ProdutoService;
import com.icecandylovers.services.VendaService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendas")
public class VendaController {

    private static final Logger logger = LoggerFactory.getLogger(VendaController.class);

    private final VendaService vendaService;
    private final ProdutoService produtoService;

    @Autowired
    public VendaController(VendaService vendaService, ProdutoService produtoService) {
        this.vendaService = vendaService;
        this.produtoService = produtoService;
    }

    @GetMapping("/nova")
    public String novaVenda(Model model) {
        logger.info("Carregando formulário para nova venda");
        try {
            model.addAttribute("produtos", produtoService.listarProdutos());
            String formattedDateTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            model.addAttribute("currentDateTime", formattedDateTime);
            model.addAttribute("venda", new VendaDTO(
                    null, null, null, null, null, LocalDateTime.now()
            ));
            model.addAttribute("currentUri", "/vendas/nova");
            return "venda-form";
        } catch (Exception e) {
            logger.error("Erro ao carregar formulário de nova venda: {}", e.getMessage(), e);
            model.addAttribute("erro", "Erro ao carregar o formulário. Tente novamente.");
            return "error"; // Supondo que exista uma página de erro
        }
    }

    @GetMapping("/editar/{id}")
    public String editarVenda(@PathVariable Long id, Model model) {
        logger.info("Carregando formulário para editar venda ID: {}", id);
        try {
            Venda venda = vendaService.buscarVendaPorId(id);
            if (venda.getItens() == null || venda.getItens().isEmpty()) {
                throw new ResourceNotFoundException("Venda não contém itens");
            }

            VendaItem item = venda.getItens().get(0);
            VendaDTO vendaDTO = new VendaDTO(
                    item.getProduto().getId(),
                    item.getQuantidade(),
                    venda.getVendido(),
                    venda.getValorUnitarioVenda(),
                    venda.getFormaPagamento(),
                    venda.getDataVenda()
            );

            model.addAttribute("produtos", produtoService.listarProdutos());
            model.addAttribute("venda", vendaDTO);
            model.addAttribute("vendaId", id);
            model.addAttribute("currentDateTime", venda.getDataVenda()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            model.addAttribute("currentUri", "/vendas/editar");
            return "venda-form";
        } catch (ResourceNotFoundException e) {
            logger.warn("Venda não encontrada ou inválida: {}", e.getMessage());
            model.addAttribute("erro", e.getMessage());
            return "error";
        } catch (Exception e) {
            logger.error("Erro ao carregar formulário de edição da venda ID {}: {}", id, e.getMessage(), e);
            model.addAttribute("erro", "Erro ao carregar o formulário. Tente novamente.");
            return "error";
        }
    }

    @PostMapping("/nova")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarVenda(@Valid @RequestBody VendaDTO vendaDTO) {
        logger.info("Registrando nova venda para produto ID: {}", vendaDTO.produtoId());
        try {
            Venda venda = vendaService.registrarVenda(vendaDTO);
            BigDecimal totalVenda = vendaDTO.valorUnitarioVenda()
                    .multiply(BigDecimal.valueOf(vendaDTO.quantidade()));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venda registrada com sucesso!");
            response.put("totalVenda", totalVenda);
            response.put("dataVenda", venda.getDataVenda().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            logger.warn("Erro ao registrar venda: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Erro interno ao registrar venda: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Erro interno ao registrar venda");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizarVenda(
            @PathVariable Long id,
            @Valid @RequestBody VendaDTO vendaDTO) { // @RequestBody adicionado novamente
        logger.info("Atualizando venda ID: {}", id);
        try {
            Venda vendaAtualizada = vendaService.atualizarVenda(id, vendaDTO);
            BigDecimal totalVenda = vendaDTO.valorUnitarioVenda()
                    .multiply(BigDecimal.valueOf(vendaDTO.quantidade()));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venda atualizada com sucesso!");
            response.put("totalVenda", totalVenda);
            response.put("dataVenda", vendaAtualizada.getDataVenda().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            logger.warn("Erro ao atualizar venda: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Erro interno ao atualizar venda ID {}: {}", id, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Erro interno ao atualizar venda");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/total-hoje")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calcularVendasHoje() {
        logger.info("Calculando total de vendas de hoje");
        try {
            BigDecimal totalVendasHoje = vendaService.calcularVendasHoje();
            Map<String, Object> response = new HashMap<>();
            response.put("totalVendasHoje", totalVendasHoje);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao calcular total de vendas de hoje: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Erro ao calcular total de vendas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarVendas() {
        logger.info("Listando todas as vendas");
        try {
            List<Venda> vendas = vendaService.listarVendas();
            List<Map<String, Object>> vendasResponse = vendas.stream().map(venda -> {
                VendaItem item = venda.getItens().get(0);
                Map<String, Object> vendaMap = new HashMap<>();
                vendaMap.put("id", venda.getId());
                vendaMap.put("dataVenda", venda.getDataVenda().toString());
                vendaMap.put("produtoId", item.getProduto().getId());
                vendaMap.put("produtoSabor", item.getProduto().getSabor());
                vendaMap.put("quantidade", item.getQuantidade());
                vendaMap.put("vendido", venda.getVendido().toString());
                vendaMap.put("formaPagamento", venda.getFormaPagamento());
                vendaMap.put("valorUnitarioVenda", venda.getValorUnitarioVenda());
                return vendaMap;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(vendasResponse);
        } catch (Exception e) {
            logger.error("Erro ao listar vendas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/deletar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletarVenda(@PathVariable Long id) {
        logger.info("Deletando venda ID: {}", id);
        try {
            vendaService.deletarVenda(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venda deletada com sucesso!");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.warn("Venda não encontrada: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("Erro ao deletar venda ID {}: {}", id, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Erro interno ao deletar venda");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
