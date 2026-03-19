package com.icecandylovers.controllers;

import com.icecandylovers.entities.Vendido;
import com.icecandylovers.services.RelatorioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    private static final Logger logger = LoggerFactory.getLogger(RelatorioController.class);

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String relatorios(Model model) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(1);
        Vendido defaultChannel = Vendido.ALL;

        Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(
                startDate,
                endDate,
                defaultChannel
        );
        model.addAllAttributes(relatorio);

        return "relatorios";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dados")
    public ResponseEntity<?> getRelatorio(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Vendido salesChannel) {

        try {
            logger.info("Gerando relatório entre {} e {} para o canal {}", startDate, endDate, salesChannel);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            Map<String, Object> relatorio = relatorioService.gerarRelatorioCompleto(startDateTime, endDateTime, salesChannel);
            return ResponseEntity.ok(relatorio);
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar relatorio. Tente novamente mais tarde.");
        }
    }
}
