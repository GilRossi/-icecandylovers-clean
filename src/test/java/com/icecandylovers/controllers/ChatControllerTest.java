package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.ChatMessageRequest;
import com.icecandylovers.services.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    // 1. Returns 200 with the service response for a valid message
    @Test
    @DisplayName("deve_retornar200_quando_mensagemValida")
    void deve_retornar200_quando_mensagemValida() throws Exception {
        String mensagemEsperada = "Oi! Bem-vindo(a) à Ice Candy Lovers!";
        when(chatService.processarMensagem("oi")).thenReturn(mensagemEsperada);

        ChatMessageRequest request = new ChatMessageRequest("oi");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(mensagemEsperada));
    }

    // 2. Returns 400 for an empty/blank message (input validation via @NotBlank)
    @Test
    @DisplayName("deve_retornar400_quando_mensagemEmBranco")
    void deve_retornar400_quando_mensagemEmBranco() throws Exception {
        ChatMessageRequest request = new ChatMessageRequest("   ");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // 3. Returns 400 for null message
    @Test
    @DisplayName("deve_retornar400_quando_mensagemNula")
    void deve_retornar400_quando_mensagemNula() throws Exception {
        // Serialize manually so that "message" is null in the JSON
        String jsonComMensagemNula = "{\"message\":null}";

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonComMensagemNula))
                .andExpect(status().isBadRequest());
    }

    // 4. Returns 200 with fallback message when service returns a fallback response (mock the service)
    @Test
    @DisplayName("deve_retornar200_com_respostaFallback_quando_servicoRetornaFallback")
    void deve_retornar200_com_respostaFallback_quando_servicoRetornaFallback() throws Exception {
        String respostaFallback = "Não sei tudo ainda, mas posso te contar sobre nossos geladinhos e docinhos. O que você quer saber?";
        when(chatService.processarMensagem(anyString())).thenReturn(respostaFallback);

        ChatMessageRequest request = new ChatMessageRequest("alguma pergunta aleatoria");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(respostaFallback));
    }

    // 5. Returns 200 with OpenAI response (mock service to return an AI response)
    @Test
    @DisplayName("deve_retornar200_com_respostaOpenAI_quando_servicoRetornaRespostaIA")
    void deve_retornar200_com_respostaOpenAI_quando_servicoRetornaRespostaIA() throws Exception {
        String respostaOpenAI = "Olá! Nossos geladinhos gourmet são feitos com ingredientes de alta qualidade. Posso te ajudar com mais informações!";
        when(chatService.processarMensagem("me fale sobre os produtos")).thenReturn(respostaOpenAI);

        ChatMessageRequest request = new ChatMessageRequest("me fale sobre os produtos");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(respostaOpenAI));
    }
}
