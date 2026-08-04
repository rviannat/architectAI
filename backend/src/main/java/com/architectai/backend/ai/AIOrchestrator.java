package com.architectai.backend.ai;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Orquestrador central de agentes especializados
 * Coordena execução, consolidação de findings e geração de relatório final
 */
public interface AIOrchestrator {

    /**
     * Executa análise completa usando múltiplos agentes
     *
     * @param repositoryPath Caminho local do repositório clonado
     * @param metadata Metadados extraídos do repositório
     * @param analysisId ID da análise
     * @return Map com results de cada agente: [agent_name] -> AgentResponse
     */
    Map<String, AgentResponse> analyzeWithAgents(Path repositoryPath, RepositoryMetadata metadata, String analysisId);

    /**
     * Retorna Tech Lead Agent consolidado (ele recebe todos os outputs)
     */
    AgentResponse getTechLeadConsolidation(Map<String, AgentResponse> agentResponses);

    /**
     * Registra um novo agente para ser executado
     */
    void registerAgent(SpecialistAgent agent);

    /**
     * Lista agentes registrados ordenados por executionOrder
     */
    List<SpecialistAgent> getRegisteredAgents();
}

