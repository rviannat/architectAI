package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaSpecialistAgent extends AbstractLlmSpecialistAgent {

    public JavaSpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Java Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especializado em Java, Spring Boot, JPA, Hibernate, transacoes, concorrencia, APIs REST e boas praticas do ecossistema Java";
    }

    @Override
    protected String getAgentType() {
        return "JAVA_SPECIALIST";
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return "java".equalsIgnoreCase(metadata.primaryLanguage()) ||
               metadata.languages().contains("java") ||
               metadata.frameworks().stream().anyMatch(f -> f.toLowerCase().contains("spring"));
    }

    @Override
    public int executionOrder() {
        return 20;
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Foque em Java 21, Spring Boot, transacoes, JPA/Hibernate, concorrencia, APIs REST e code smells de backend.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".xml", ".properties", ".yml", ".yaml", "pom.xml", "build.gradle");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("pom.xml", "build.gradle", "build.gradle.kts");
    }
}

