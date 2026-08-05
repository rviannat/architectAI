package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardReportingAgent extends AbstractLlmSpecialistAgent {

    public BoardReportingAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Board Reporting Agent";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em narrativa executiva para diretoria e investidores com KPIs de risco, custo e velocidade de entrega.";
    }

    @Override
    protected String getAgentType() {
        return "BOARD_REPORTING_AGENT";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Transforme os achados tecnicos e comerciais em narrativa de board report. Destaque indicadores de risco operacional, custo evitado, impacto na receita, horizonte de execucao e governanca. Forneca recomendacoes acionaveis para reunioes de diretoria e investidores em formato objetivo.";
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
        return 82;
    }

    @Override
    public String getAgentDomain() {
        return "COMMERCIAL";
    }
}

