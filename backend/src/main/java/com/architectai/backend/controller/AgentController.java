package com.architectai.backend.controller;

import com.architectai.backend.ai.AIOrchestrator;
import com.architectai.backend.ai.SpecialistAgent;
import com.architectai.backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AIOrchestrator aiOrchestrator;

    public AgentController(AIOrchestrator aiOrchestrator) {
        this.aiOrchestrator = aiOrchestrator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAgents() {
        List<Map<String, Object>> agents = aiOrchestrator.getRegisteredAgents().stream()
            .map(this::toAgentPayload)
            .toList();

        long technicalCount = aiOrchestrator.getRegisteredAgents().stream()
            .filter(agent -> "TECHNICAL".equalsIgnoreCase(agent.getAgentDomain()))
            .count();
        long commercialCount = aiOrchestrator.getRegisteredAgents().stream()
            .filter(agent -> "COMMERCIAL".equalsIgnoreCase(agent.getAgentDomain()))
            .count();

        return ResponseEntity.ok(ApiResponse.of(200, "Agents listed", Map.of(
            "count", agents.size(),
            "technicalCount", technicalCount,
            "commercialCount", commercialCount,
            "agents", agents,
            "techLead", "Tech Lead"
        )));
    }

    private Map<String, Object> toAgentPayload(SpecialistAgent agent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", agent.getAgentName());
        payload.put("description", agent.getAgentDescription());
        payload.put("order", agent.executionOrder());
        payload.put("domain", agent.getAgentDomain());
        return payload;
    }
}

