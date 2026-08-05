package com.architectai.backend.ai.llm;

import com.architectai.backend.ai.AIProvider;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class DefaultLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultLlmClient.class);
    private static final String JSON_FALLBACK = "{\"findings\":[],\"recommendations\":[],\"summary\":\"No model response available\"}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AgentAiProperties properties;

    public DefaultLlmClient(RestTemplate restTemplate, ObjectMapper objectMapper, AgentAiProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String askForJson(AIProvider provider, String systemPrompt, String userPrompt) {
        return switch (provider) {
            case OPENAI -> callOpenAi(systemPrompt, userPrompt);
            case ANTHROPIC -> callAnthropic(systemPrompt, userPrompt);
            case GEMINI -> callGemini(systemPrompt, userPrompt);
            case OLLAMA -> JSON_FALLBACK;
        };
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return JSON_FALLBACK;
        }

        String endpoint = properties.getOpenai().getBaseUrl() + "/v1/chat/completions";
        Map<String, Object> payload = Map.of(
            "model", properties.getOpenai().getModel(),
            "temperature", 0.2,
            "response_format", Map.of("type", "json_object"),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(payload, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText(JSON_FALLBACK);
        } catch (Exception e) {
            log.warn("Falha ao chamar OpenAI: {}", e.getMessage());
            return JSON_FALLBACK;
        }
    }

    private String callAnthropic(String systemPrompt, String userPrompt) {
        String apiKey = properties.getAnthropic().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return JSON_FALLBACK;
        }

        String endpoint = properties.getAnthropic().getBaseUrl() + "/v1/messages";
        Map<String, Object> payload = Map.of(
            "model", properties.getAnthropic().getModel(),
            "max_tokens", 3000,
            "temperature", 0.2,
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(payload, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText(JSON_FALLBACK);
            }
            return JSON_FALLBACK;
        } catch (Exception e) {
            log.warn("Falha ao chamar Anthropic: {}", e.getMessage());
            return JSON_FALLBACK;
        }
    }

    private String callGemini(String systemPrompt, String userPrompt) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return JSON_FALLBACK;
        }

        String endpoint = properties.getGemini().getBaseUrl() + "/v1beta/models/"
            + properties.getGemini().getModel() + ":generateContent?key=" + apiKey;
        Map<String, Object> payload = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", systemPrompt + "\n\n" + userPrompt))))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(payload, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(JSON_FALLBACK);
        } catch (Exception e) {
            log.warn("Falha ao chamar Gemini: {}", e.getMessage());
            return JSON_FALLBACK;
        }
    }
}
