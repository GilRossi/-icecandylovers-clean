package com.icecandylovers.controllers;

import com.icecandylovers.exceptions.DuplicateResourceException;
import com.icecandylovers.exceptions.InsufficientStockException;
import com.icecandylovers.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalRestExceptionHandlerTest {

    private GlobalRestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalRestExceptionHandler();
    }

    @Test
    void handleResourceNotFound_returns404() {
        var ex = new ResourceNotFoundException("Produto nao encontrado");
        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("Produto nao encontrado", response.getBody().get("error"));
    }

    @Test
    void handleDuplicateResource_returns409() {
        var ex = new DuplicateResourceException("Ja existe");
        ResponseEntity<Map<String, Object>> response = handler.handleDuplicateResource(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("Ja existe", response.getBody().get("error"));
    }

    @Test
    void handleInsufficientStock_returns422() {
        var ex = new InsufficientStockException("Estoque insuficiente");
        ResponseEntity<Map<String, Object>> response = handler.handleInsufficientStock(ex);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("Estoque insuficiente", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_returns400() {
        var ex = new IllegalArgumentException("Argumento invalido");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("Argumento invalido", response.getBody().get("error"));
    }

    @Test
    void handleUnexpectedException_returns500_withGenericMessage() {
        var ex = new RuntimeException("Detalhe interno sensivel");
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpectedException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
        assertEquals("Erro interno do servidor", response.getBody().get("error"));
    }

    @Test
    void handleConstraintViolation_returns400() {
        // ConstraintViolationException requires a Set of violations
        var ex = new jakarta.validation.ConstraintViolationException("Campo invalido", null);
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().get("success"));
    }
}
