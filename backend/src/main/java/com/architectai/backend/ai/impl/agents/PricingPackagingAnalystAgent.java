package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PricingPackagingAnalystAgent extends AbstractLlmSpecialistAgent {

    public PricingPackagingAnalystAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Pricing & Packaging Analyst";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em precificacao por valor, estrutura de pacotes e previsibilidade de margem.";
    }

    @Override
    protected String getAgentType() {
        return "PRICING_PACKAGING_ANALYST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Defina recomendacoes de precificacao por valor com pacotes (Essencial, Avancado, Enterprise), considerando risco tecnico, esforco estimado e retorno para o cliente. Destaque impactos em margem, CAC payback e possibilidade de upsell.";
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
        return 75;
    }

    @Override
    public String getAgentDomain() {
        return "COMMERCIAL";
    }
}

