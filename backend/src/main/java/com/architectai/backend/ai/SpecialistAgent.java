package com.architectai.backend.ai;

import java.nio.file.Path;

/**
 * Interface base para todos os agentes especializados
 */
public interface SpecialistAgent {

    /**
     * Retorna o nome/tipo do agente
     */
    String getAgentName();

    /**
     * Retorna descrição breve do agente
     */
    String getAgentDescription();

    /**
     * Analisa um repositório clonado e retorna findings estruturados
     *
     * @param repositoryPath Caminho local do repositório clonado
     * @param analysisId ID da análise
     * @return AgentResponse estruturado com findings, recomendações, etc
     */
    AgentResponse analyze(Path repositoryPath, String analysisId);

    /**
     * Retorna se o agente pode processar este tipo de repositório
     */
    boolean canHandle(RepositoryMetadata metadata);

    /**
     * Retorna a ordem de execução (0 = primeiro, 100 = Tech Lead consolida)
     */
    int executionOrder();
}

