package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoftwareArchitectAgent extends AbstractLlmSpecialistAgent {

    public SoftwareArchitectAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Software Architect";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em DDD, SOLID, Clean Architecture, Hexagonal e modularizacao.";
    }

    @Override
    protected String getAgentType() {
        return "SOFTWARE_ARCHITECT";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Analise boundaries, acoplamento, coesao, violacoes SOLID, anti-patterns arquiteturais e divida tecnica estrutural.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".kt", ".xml", ".yaml", ".yml", ".md");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return metadata.fileCount() > 0;
    }

    @Override
    public int executionOrder() {
        return 10;
    }
}

