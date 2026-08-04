package com.architectai.backend.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Schema padronizado para outputs de agentes especializados
 * Todos os agentes retornam neste formato
 */
public record AgentResponse(
    @JsonProperty("agent_name")
    String agentName,

    @JsonProperty("agent_type")
    String agentType,

    @JsonProperty("timestamp")
    long timestamp,

    @JsonProperty("execution_time_ms")
    long executionTimeMs,

    @JsonProperty("status")
    String status,  // SUCCESS, PARTIAL, FAILED

    @JsonProperty("findings")
    List<Finding> findings,

    @JsonProperty("summary")
    String summary,

    @JsonProperty("recommendations")
    List<String> recommendations,

    @JsonProperty("metadata")
    Map<String, Object> metadata
) {

    /**
     * Representa um achado individual (problema detectado)
     */
    public record Finding(
        @JsonProperty("id")
        String id,

        @JsonProperty("type")
        String type,  // ARCHITECTURE, CODE_QUALITY, SECURITY, PERFORMANCE, etc

        @JsonProperty("severity")
        String severity,  // CRITICAL, HIGH, MEDIUM, LOW, INFO

        @JsonProperty("file")
        String file,

        @JsonProperty("line")
        Integer line,

        @JsonProperty("title")
        String title,

        @JsonProperty("description")
        String description,

        @JsonProperty("recommendation")
        String recommendation,

        @JsonProperty("estimated_effort_hours")
        Integer estimatedEffortHours,

        @JsonProperty("confidence")
        Double confidence,  // 0.0 - 1.0

        @JsonProperty("tags")
        List<String> tags,

        @JsonProperty("references")
        List<String> references
    ) {}
}

