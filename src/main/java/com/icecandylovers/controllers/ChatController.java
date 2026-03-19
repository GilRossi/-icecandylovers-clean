package com.icecandylovers.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icecandylovers.dtos.ChatMessageRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final int MAX_CACHE_SIZE = 500;

    private final Map<String, String> respostasProntas = new HashMap<>();
    private final Map<String, String> cacheRespostas = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    @Value("${chat.openai.enabled:false}")
    private boolean openAiEnabled;

    @Value("${chat.openai.api-key:}")
    private String apiKey;

    @Value("${chat.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${chat.openai.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public ChatController(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        popularRespostasProntas();
    }

    @PostMapping("/message")
    public ResponseEntity<String> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        String mensagemOriginal = request.message().trim();
        String mensagemNormalizada = normalizar(mensagemOriginal.toLowerCase());

        String respostaPronta = respostasProntas.get(mensagemNormalizada);
        if (respostaPronta != null) {
            return ResponseEntity.ok(respostaPronta);
        }

        String respostaCache = cacheRespostas.get(mensagemNormalizada);
        if (respostaCache != null) {
            return ResponseEntity.ok(respostaCache);
        }

        String respostaApi = chamarOpenAi(mensagemOriginal);
        if (respostaApi.startsWith("Ops,")) {
            return ResponseEntity.ok(fallbackResposta(mensagemNormalizada));
        }

        cacheRespostas.put(mensagemNormalizada, respostaApi);
        return ResponseEntity.ok(respostaApi);
    }

    private String chamarOpenAi(String message) {
        if (!openAiEnabled || !StringUtils.hasText(apiKey)) {
            logger.debug("Integração OpenAI desabilitada ou sem chave configurada.");
            return "Ops, meu modo inteligente está indisponível no momento.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "Você é o assistente virtual da Ice Candy Lovers. Responda de forma curta, educada e focada nos produtos da loja."),
                        Map.of("role", "user", "content", message)
                },
                "max_tokens", 120
        );

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode contentNode = jsonNode.path("choices").path(0).path("message").path("content");

            if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
                logger.warn("Resposta vazia recebida da OpenAI.");
                return "Ops, minha conexão congelou! Tente novamente.";
            }

            return contentNode.asText().trim();
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.warn("OpenAI retornou limite de taxa.");
            return "Ops, estou muito popular hoje! Tente novamente em alguns minutos.";
        } catch (HttpClientErrorException e) {
            logger.error("Erro HTTP ao consultar OpenAI: {}", e.getStatusCode(), e);
            return "Ops, minha conexão congelou! Tente novamente.";
        } catch (Exception e) {
            logger.error("Erro ao consultar OpenAI.", e);
            return "Ops, algo deu errado na minha geladeira virtual. Tente novamente!";
        }
    }

    private void popularRespostasProntas() {
        respostasProntas.put(normalizar("quem é gelyto"), "Eu sou o Gelyto, o assistente virtual da Ice Candy Lovers! Estou aqui para te ajudar a descobrir nossos geladinhos gourmet e doces incríveis.");
        respostasProntas.put(normalizar("oi"), "Oi! Bem-vindo(a) à Ice Candy Lovers! Quer saber sobre nossos sabores, preços ou como pedir?");
        respostasProntas.put(normalizar("olá"), "Olá! Aqui é o Gelyto. Posso te contar sobre nossos geladinhos gourmet, docinhos e até os alcoólicos.");
        respostasProntas.put(normalizar("como pedir"), "Você pode nos contactar pelo Instagram @icecandyloverspg ou pelo email icecandyloverspg@gmail.com.");
        respostasProntas.put(normalizar("produtos"), "Temos geladinhos gourmet, geladinhos alcoólicos e docinhos gourmet. Sobre qual deles você quer saber mais?");
        respostasProntas.put(normalizar("sabores"), getSaboresResponse());
        respostasProntas.put(normalizar("geladinho gourmet"), "Nossos geladinhos gourmet são feitos com ingredientes premium, com sabores como ninho com nutella, morango, maracujá e muito mais.");
        respostasProntas.put(normalizar("geladinho alcoolico"), "Temos opções como espanhola, 51 limão ice e VB ice limão.");
        respostasProntas.put(normalizar("docinhos"), "A Caixinha da Felicidade vem com 4 doces: brigadeiro gourmet, beijinho, ninho e ferrer rocher.");
        respostasProntas.put(normalizar("preco"), "Geladinhos: R$15,00 cada. Caixinha de doces: R$18,00.");
        respostasProntas.put(normalizar("entrega"), "Entregamos em Ponta Grossa. Consulte-nos via Instagram ou email para combinar.");
        respostasProntas.put(normalizar("contato"), "Nos encontre no Instagram @icecandyloverspg ou por email: icecandyloverspg@gmail.com.");
        respostasProntas.put(normalizar("qual é o seu nome"), "Meu nome é Gelyto! Sou o assistente virtual da Ice Candy Lovers.");
    }

    private String fallbackResposta(String mensagemNormalizada) {
        if (mensagemNormalizada.contains("sabor") || mensagemNormalizada.contains("geladinho") ||
                mensagemNormalizada.contains("doce") || mensagemNormalizada.contains("alcool")) {
            return "Nossos sabores incluem ninho com nutella, ninho, maracujá, morango, prestigio, coco e também opções alcoólicas e docinhos gourmet.";
        }

        if (mensagemNormalizada.contains("preco") || mensagemNormalizada.contains("custa") || mensagemNormalizada.contains("valor")) {
            return "Geladinhos por R$ 15,00 e docinhos gourmet por R$ 18,00 a caixinha.";
        }

        if (mensagemNormalizada.contains("nome") || mensagemNormalizada.contains("quem")) {
            return "Eu sou o Gelyto, o assistente virtual da Ice Candy Lovers!";
        }

        return "Não sei tudo ainda, mas posso te contar sobre nossos geladinhos e docinhos. O que você quer saber?";
    }

    private String getSaboresResponse() {
        return """
                Nossos sabores são:

                Geladinhos Gourmet: Ninho com Nutella, Morango, Maracujá, Coco e Prestigio.
                Geladinhos Alcoólicos: Espanhola, 51 Limão Ice e VB Ice Limão.
                Caixinha da Felicidade: Brigadeiro, Beijinho, Ninho e Ferrer Rocher.
                """;
    }

    private String normalizar(String texto) {
        return texto.toLowerCase()
                .replaceAll("[áàãâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòõôö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("ç", "c")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }
}
