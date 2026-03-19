package com.icecandylovers.controllers;

import com.icecandylovers.dtos.PromocaoDTO;
import com.icecandylovers.services.PromocaoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/promocoes")
public class PromocaoController {

    private static final Logger logger = LoggerFactory.getLogger(PromocaoController.class);

    private final PromocaoService promocaoService;

    public PromocaoController(PromocaoService promocaoService) {
        this.promocaoService = promocaoService;
    }

    @GetMapping
    public String mostrarPagina() {
        return "promocoes";
    }

    @GetMapping("/listar")
    @ResponseBody
    public ResponseEntity<List<PromocaoDTO>> listar() {
        logger.info("Listando todas as promoções");
        return ResponseEntity.ok(promocaoService.listarTodas());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/salvar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> criar(@Valid @RequestBody PromocaoDTO dto) {
        logger.info("Criando promoção: {}", dto.nome());
        PromocaoDTO criada = promocaoService.criarPromocao(dto);
        return ResponseEntity.ok(Map.of("success", true, "id", criada.id()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id, @Valid @RequestBody PromocaoDTO dto) {
        logger.info("Editando promoção ID: {}", id);
        PromocaoDTO atualizada = promocaoService.atualizarPromocao(id, dto);
        return ResponseEntity.ok(Map.of("success", true, "id", atualizada.id()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deletar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletar(@PathVariable Long id) {
        logger.info("Deletando promoção ID: {}", id);
        promocaoService.deletarPromocao(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/ativar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ativarDesativar(@PathVariable Long id, @RequestParam boolean ativo) {
        logger.info("Alterando status da promoção ID: {} para ativo={}", id, ativo);
        PromocaoDTO atualizada = promocaoService.ativarDesativar(id, ativo);
        return ResponseEntity.ok(Map.of("success", true, "ativo", atualizada.ativo()));
    }

    @GetMapping("/produto/{produtoId}")
    @ResponseBody
    public ResponseEntity<List<PromocaoDTO>> promocoesPorProduto(@PathVariable Long produtoId) {
        logger.info("Buscando promoções para produto ID: {}", produtoId);
        return ResponseEntity.ok(promocaoService.buscarPromocoesParaProduto(produtoId));
    }
}
