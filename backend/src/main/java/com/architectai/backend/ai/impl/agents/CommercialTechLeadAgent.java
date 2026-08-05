package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommercialTechLeadAgent extends AbstractLlmSpecialistAgent {

    public CommercialTechLeadAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Commercial Tech Lead";
    }

    @Override
    public String getAgentDescription() {
        return "Orquestrador executivo comercial para consolidar valor tecnico em plano de decisao para C-level.";
    }

    @Override
    protected String getAgentType() {
        return "COMMERCIAL_TECH_LEAD";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Consolide as evidencias tecnicas em narrativa executiva para CEO, CTO e CFO. Priorize recomendacoes por impacto financeiro, risco operacional e velocidade de entrega. Estruture roadmap comercial 30/60/90 dias com proximos passos para contratacao e expansao.";
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
        return 90;
    }

    @Override
    public String getAgentDomain() {
        return "COMMERCIAL";
    }
}

