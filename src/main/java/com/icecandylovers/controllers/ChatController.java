package com.icecandylovers.controllers;

import com.icecandylovers.dtos.ChatMessageRequest;
import com.icecandylovers.services.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ResponseEntity<String> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        logger.info("Mensagem recebida no chat");
        String resposta = chatService.processarMensagem(request.message());
        return ResponseEntity.ok(resposta);
    }
}
