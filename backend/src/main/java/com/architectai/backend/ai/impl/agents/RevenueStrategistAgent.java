package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RevenueStrategistAgent extends AbstractLlmSpecialistAgent {

    public RevenueStrategistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Revenue Strategist";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em conectar achados tecnicos com receita incremental, risco financeiro e plano de expansao de conta.";
    }

    @Override
    protected String getAgentType() {
        return "REVENUE_STRATEGIST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Converta evidencias tecnicas em oportunidades comerciais. Priorize recomendacoes por impacto em receita, margem, reducao de churn e risco financeiro. Crie orientacoes para upsell, cross-sell e pacote mensal de acompanhamento com estimativa de ROI em 30/60/90 dias.";
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
        return 72;
    }

    @Override
    public String getAgentDomain() {
        return "COMMERCIAL";
    }
}

