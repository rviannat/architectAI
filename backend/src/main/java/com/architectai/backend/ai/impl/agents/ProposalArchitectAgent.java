package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProposalArchitectAgent extends AbstractLlmSpecialistAgent {

    public ProposalArchitectAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Proposal Architect";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em transformar diagnostico tecnico em proposta comercial executiva com escopo, valor e cronograma.";
    }

    @Override
    protected String getAgentType() {
        return "PROPOSAL_ARCHITECT";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Converta riscos tecnicos em proposta comercial objetiva. Priorize entregas por valor de negocio, monte pacotes por fase (30/60/90 dias), explicite impacto em custo, risco e velocidade de entrega. Gere recomendacoes orientadas a fechamento executivo.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".kt", ".yml", ".yaml", ".md", ".xml", ".json", ".sql");
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
        return 70;
    }

    @Override
    public String getAgentDomain() {
        return "COMMERCIAL";
    }
}

