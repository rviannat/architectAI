package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DatabaseSpecialistAgent extends AbstractLlmSpecialistAgent {

    public DatabaseSpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Database Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em modelagem, indices, queries e desempenho de bancos relacionais e NoSQL.";
    }

    @Override
    protected String getAgentType() {
        return "DATABASE_SPECIALIST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Analise modelagem, normalizacao, indices ausentes, lock contention, N+1, particionamento e politicas de cache no banco.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".sql", ".java", ".xml", ".yml", ".yaml", ".properties");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("schema.sql", "data.sql", "liquibase.properties", "flyway.conf", "application.yml", "application.properties");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        if (metadata.databaseType() != null && !metadata.databaseType().isBlank() && !"unknown".equalsIgnoreCase(metadata.databaseType())) {
            return true;
        }
        return metadata.dependencies().keySet().stream().map(v -> v.toLowerCase(Locale.ROOT)).anyMatch(dep ->
            dep.contains("postgres") || dep.contains("mysql") || dep.contains("oracle") || dep.contains("mongo") || dep.contains("redis")
        );
    }

    @Override
    public int executionOrder() {
        return 60;
    }
}

