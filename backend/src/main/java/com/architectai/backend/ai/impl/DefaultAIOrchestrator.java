package com.architectai.backend.ai.impl;

import com.architectai.backend.ai.*;
import com.architectai.backend.ai.impl.agents.TechLeadAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementação do orquestrador de agentes
 */
@Component
public class DefaultAIOrchestrator implements AIOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(DefaultAIOrchestrator.class);

    private final List<SpecialistAgent> agents;
    private final TechLeadAgent techLeadAgent;

    public DefaultAIOrchestrator(List<SpecialistAgent> specialistAgents, TechLeadAgent techLeadAgent) {
        this.agents = new ArrayList<>(specialistAgents);
        this.agents.sort(Comparator.comparingInt(SpecialistAgent::executionOrder));
        this.techLeadAgent = techLeadAgent;
    }

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
                
                AgentResponse response = agent.analyze(repositoryPath, metadata, analysisId);
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
        log.info("Consolidando {} respostas de agentes com Tech Lead...", agentResponses.size());
        return techLeadAgent.consolidate(agentResponses, "consolidation");
    }

    @Override
    public void registerAgent(SpecialistAgent agent) {
        boolean exists = agents.stream().anyMatch(a -> a.getAgentName().equalsIgnoreCase(agent.getAgentName()));
        if (exists) {
            return;
        }
        agents.add(agent);
        agents.sort(Comparator.comparingInt(SpecialistAgent::executionOrder));
        log.info("Agente registrado: {} (ordem: {})", agent.getAgentName(), agent.executionOrder());
    }

    @Override
    public List<SpecialistAgent> getRegisteredAgents() {
        return new ArrayList<>(agents);
    }

}
