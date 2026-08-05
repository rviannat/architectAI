package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.AgentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AgentResponseParser {
    private static final Logger log = LoggerFactory.getLogger(AgentResponseParser.class);

    private final ObjectMapper objectMapper;

    public AgentResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AgentResponse parse(String rawResponse, String agentName, String agentType, long executionTimeMs, Map<String, Object> metadata) {
        try {
            JsonNode root = readJsonFromRaw(rawResponse);
            List<AgentResponse.Finding> findings = parseFindings(root.path("findings"));
            List<String> recommendations = parseRecommendations(root.path("recommendations"));
            String summary = root.path("summary").asText(defaultSummary(findings.size()));

            return new AgentResponse(
                agentName,
                agentType,
                System.currentTimeMillis(),
                executionTimeMs,
                "SUCCESS",
                findings,
                summary,
                recommendations,
                metadata
            );
        } catch (Exception e) {
            log.warn("Nao foi possivel parsear resposta do agente {}: {}", agentName, e.getMessage());
            return new AgentResponse(
                agentName,
                agentType,
                System.currentTimeMillis(),
                executionTimeMs,
                "PARTIAL",
                List.of(),
                "Resposta do modelo sem JSON valido; revisar logs para detalhes.",
                List.of("Executar novamente com contexto menor e validar prompt JSON"),
                metadata
            );
        }
    }

    private JsonNode readJsonFromRaw(String rawResponse) throws Exception {
        String value = rawResponse == null ? "" : rawResponse.trim();
        if (value.startsWith("```")) {
            int first = value.indexOf('{');
            int last = value.lastIndexOf('}');
            if (first >= 0 && last > first) {
                value = value.substring(first, last + 1);
            }
        }
        return objectMapper.readTree(value);
    }

    private List<AgentResponse.Finding> parseFindings(JsonNode findingsNode) {
        List<AgentResponse.Finding> findings = new ArrayList<>();
        if (!findingsNode.isArray()) {
            return findings;
        }

        for (JsonNode node : findingsNode) {
            String id = safeText(node, "id");
            if (id.isBlank()) {
                id = "F-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            }

            findings.add(new AgentResponse.Finding(
                id,
                safeTextOr(node, "type", "CODE_QUALITY"),
                normalizeSeverity(safeTextOr(node, "severity", "MEDIUM")),
                safeTextOr(node, "file", "unknown"),
                node.hasNonNull("line") ? node.get("line").asInt() : null,
                safeTextOr(node, "title", "Finding sem titulo"),
                safeTextOr(node, "description", "Sem descricao"),
                safeTextOr(node, "recommendation", "Sem recomendacao"),
                node.hasNonNull("estimated_effort_hours") ? node.get("estimated_effort_hours").asInt() : 4,
                node.hasNonNull("confidence") ? node.get("confidence").asDouble() : 0.7,
                toStringList(node.path("tags")),
                toStringList(node.path("references"))
            ));
        }

        return findings;
    }

    private List<String> parseRecommendations(JsonNode recommendationsNode) {
        List<String> recommendations = toStringList(recommendationsNode);
        if (recommendations.isEmpty()) {
            recommendations.add("Priorizar os findings CRITICAL e HIGH em ate 2 sprints.");
        }
        return recommendations;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode element : node) {
            if (element.isTextual()) {
                values.add(element.asText());
            }
        }
        return values;
    }

    private String safeText(JsonNode node, String key) {
        return node.path(key).asText("").trim();
    }

    private String safeTextOr(JsonNode node, String key, String fallback) {
        String value = safeText(node, key);
        return value.isBlank() ? fallback : value;
    }

    private String normalizeSeverity(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO" -> upper;
            default -> "MEDIUM";
        };
    }

    private String defaultSummary(int findingCount) {
        return "Analise concluida com " + findingCount + " findings estruturados.";
    }
}

