package com.architectai.backend.ai.impl;

import com.architectai.backend.ai.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementação do orquestrador de agentes
 */
@Slf4j
@Component
public class DefaultAIOrchestrator implements AIOrchestrator {

    private final List<SpecialistAgent> agents = new ArrayList<>();

    @Override
    public Map<String, AgentResponse> analyzeWithAgents(Path repositoryPath, RepositoryMetadata metadata, String analysisId) {
        log.info("Iniciando análise com {} agentes para {}", agents.size(), analysisId);
        
        Map<String, AgentResponse> results = new HashMap<>();
        
        // Executar cada agente em ordem
        for (SpecialistAgent agent : agents) {
            if (!agent.canHandle(metadata)) {
                log.debug("Agente {} não pode processar este repositório", agent.getAgentName());
                continue;
            }
            
            try {
                long start = System.currentTimeMillis();
                log.info("Executando agente: {}", agent.getAgentName());
                
                AgentResponse response = agent.analyze(repositoryPath, analysisId);
                results.put(agent.getAgentName(), response);
                
                long duration = System.currentTimeMillis() - start;
                log.info("Agente {} completado em {}ms com {} findings", 
                    agent.getAgentName(), duration, response.findings().size());
                    
            } catch (Exception e) {
                log.error("Erro ao executar agente {}: {}", agent.getAgentName(), e.getMessage(), e);
                // Continuar com próximo agente mesmo se um falhar
            }
        }
        
        log.info("Análise concluída: {} agentes executados", results.size());
        return results;
    }

    @Override
    public AgentResponse getTechLeadConsolidation(Map<String, AgentResponse> agentResponses) {
        log.info("Consolidando {} respostas de agentes...", agentResponses.size());
        
        // Consolidar todos os findings
        List<AgentResponse.Finding> consolidatedFindings = agentResponses.values().stream()
            .flatMap(response -> response.findings().stream())
            .sorted(Comparator
                .comparing((AgentResponse.Finding f) -> severityToInt(f.severity()))
                .reversed()
                .thenComparing(AgentResponse.Finding::confidence, Comparator.reverseOrder())
            )
            .limit(100)  // Top 100 findings
            .collect(Collectors.toList());
        
        // Consolidar recomendações
        List<String> consolidatedRecommendations = agentResponses.values().stream()
            .flatMap(response -> response.recommendations().stream())
            .distinct()
            .collect(Collectors.toList());
        
        // Gerar sumário executivo
        String summary = generateExecutiveSummary(agentResponses, consolidatedFindings);
        
        return new AgentResponse(
            "Tech Lead",
            "ORCHESTRATOR",
            System.currentTimeMillis(),
            0,
            "SUCCESS",
            consolidatedFindings,
            summary,
            consolidatedRecommendations,
            Map.of(
                "agent_count", agentResponses.size(),
                "total_findings", consolidatedFindings.size(),
                "critical_count", (int) consolidatedFindings.stream()
                    .filter(f -> "CRITICAL".equals(f.severity()))
                    .count()
            )
        );
    }

    @Override
    public void registerAgent(SpecialistAgent agent) {
        agents.add(agent);
        agents.sort(Comparator.comparingInt(SpecialistAgent::executionOrder));
        log.info("Agente registrado: {} (ordem: {})", agent.getAgentName(), agent.executionOrder());
    }

    @Override
    public List<SpecialistAgent> getRegisteredAgents() {
        return new ArrayList<>(agents);
    }

    private int severityToInt(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String generateExecutiveSummary(Map<String, AgentResponse> agentResponses, List<AgentResponse.Finding> findings) {
        StringBuilder sb = new StringBuilder();
        
        long criticalCount = findings.stream().filter(f -> "CRITICAL".equals(f.severity())).count();
        long highCount = findings.stream().filter(f -> "HIGH".equals(f.severity())).count();
        
        sb.append("Análise técnica completa realizada por ").append(agentResponses.size()).append(" especialistas.\n\n");
        sb.append(String.format("Encontrados %d problemas críticos e %d problemas altos.", criticalCount, highCount)).append("\n\n");
        
        // Top 3 recomendações
        agentResponses.values().stream()
            .flatMap(r -> r.recommendations().stream())
            .distinct()
            .limit(3)
            .forEach(rec -> sb.append("• ").append(rec).append("\n"));
        
        return sb.toString();
    }
}

