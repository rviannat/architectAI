package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.AIProvider;
import com.architectai.backend.ai.AgentResponse;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TechLeadAgent {

    private final LlmClient llmClient;
    private final AgentAiProperties properties;
    private final AgentResponseParser parser;

    public TechLeadAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.parser = new AgentResponseParser(objectMapper);
    }

    public AgentResponse consolidate(Map<String, AgentResponse> responses, String analysisId) {
        long start = System.currentTimeMillis();

        List<AgentResponse.Finding> merged = responses.values().stream()
            .flatMap(r -> r.findings().stream())
            .collect(Collectors.toCollection(ArrayList::new));

        if (merged.isEmpty()) {
            return new AgentResponse(
                "Tech Lead",
                "TECH_LEAD",
                System.currentTimeMillis(),
                System.currentTimeMillis() - start,
                "SUCCESS",
                List.of(),
                "Nenhum finding consolidado.",
                List.of("Executar nova analise com mais contexto de codigo."),
                Map.of("source_agents", responses.size())
            );
        }

        merged.sort(Comparator
            .comparing((AgentResponse.Finding f) -> severityRank(f.severity())).reversed()
            .thenComparing(AgentResponse.Finding::confidence, Comparator.reverseOrder()));

        Map<String, AgentResponse> topResponses = responses.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String systemPrompt = "You are a principal tech lead. Consolidate findings, remove duplicates, and prioritize by business impact. Return only valid JSON.";
        String userPrompt = buildPrompt(topResponses, merged, analysisId);
        AIProvider provider = resolveProvider();
        String raw = llmClient.askForJson(provider, systemPrompt, userPrompt);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_agents", responses.size());
        metadata.put("raw_findings", merged.size());
        metadata.put("provider", provider.getId());

        return parser.parse(raw, "Tech Lead", "TECH_LEAD", System.currentTimeMillis() - start, metadata);
    }

    private String buildPrompt(Map<String, AgentResponse> responses, List<AgentResponse.Finding> merged, String analysisId) {
        String findingsSummary = merged.stream()
            .limit(80)
            .map(f -> "%s|%s|%s|%s|%s".formatted(f.severity(), f.type(), f.file(), f.title(), f.recommendation()))
            .collect(Collectors.joining("\n"));

        return """
            ANALYSIS ID: %s
            AGENT OUTPUT COUNT: %d

            Consolidate duplicated findings with same root cause.
            Build an executive summary and a prioritized roadmap.
            Keep CRITICAL and HIGH findings first.

            FINDINGS INPUT:
            %s
            """.formatted(analysisId, responses.size(), findingsSummary);
    }

    private AIProvider resolveProvider() {
        String configured = properties.getDefaultProvider();
        if (configured != null) {
            for (AIProvider value : AIProvider.values()) {
                if (value.getId().equalsIgnoreCase(configured)) {
                    return value;
                }
            }
        }
        return AIProvider.OPENAI;
    }

    private int severityRank(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return 5;
        }
        if ("HIGH".equalsIgnoreCase(severity)) {
            return 4;
        }
        if ("MEDIUM".equalsIgnoreCase(severity)) {
            return 3;
        }
        if ("LOW".equalsIgnoreCase(severity)) {
            return 2;
        }
        return 1;
    }
}

