package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExecutiveAccountManagerAgent extends AbstractLlmSpecialistAgent {

    public ExecutiveAccountManagerAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Executive Account Manager";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em relacionamento C-level, plano de sucesso 30/60/90 dias e estrategia de expansao de conta.";
    }

    @Override
    protected String getAgentType() {
        return "EXECUTIVE_ACCOUNT_MANAGER";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Consolide os riscos tecnicos e transforme em plano executivo para CTO, CIO e CFO. Defina prioridades de governanca, marcos de adocao e indicadores de sucesso do cliente. Sugira cadencia de QBR e estrategias de retencao para reduzir churn e ampliar a conta com base em valor entregue.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".kt", ".yml", ".yaml", ".md", ".json", ".xml", ".sql");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("README.md", "pom.xml", "build.gradle", "build.gradle.kts", "docker-compose.yml", "application.yml", "application.properties");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return metadata.fileCount() > 0;
    }

    @Override
    public int executionOrder() {
        return 78;
    }

    @Override
    public String getAgentDomain() {
        return "COMMERCIAL";
    }
}

