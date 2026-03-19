package com.icecandylovers.controllers;

import com.icecandylovers.services.ProdutoService;
import com.icecandylovers.services.VendaService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final ProdutoService produtoService;
    private final VendaService vendaService;

    @Autowired
    public DashboardController(ProdutoService produtoService, VendaService vendaService) {
        this.produtoService = produtoService;
        this.vendaService = vendaService;
    }

    @GetMapping
    public String showDashboard(Model model, HttpServletRequest request) {
        logger.info("Exibindo dashboard na URI: {}", request.getRequestURI());
        try {
            model.addAttribute("currentUri", request.getRequestURI());
            model.addAttribute("produtos", produtoService.listarProdutos());
            model.addAttribute("vendasRecentes", vendaService.obterVendasRecentes());
            model.addAttribute("totalEstoque", produtoService.calcularTotalEstoque());
            model.addAttribute("vendasHoje", vendaService.calcularVendasHoje());
            return "dashboard";
        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard: {}", e.getMessage(), e);
            model.addAttribute("erro", "Erro ao carregar o dashboard. Tente novamente mais tarde.");
            return "error"; // Supondo que exista uma página de erro genérica
        }
    }
}