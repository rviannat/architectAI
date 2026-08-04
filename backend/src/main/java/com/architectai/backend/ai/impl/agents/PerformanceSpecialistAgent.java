package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PerformanceSpecialistAgent extends AbstractLlmSpecialistAgent {

    public PerformanceSpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Performance Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em JVM, GC, CPU, memoria, SQL, cache e throughput.";
    }

    @Override
    protected String getAgentType() {
        return "PERFORMANCE_SPECIALIST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Foque em hotspots de CPU/memoria, alocacao excessiva, GC pressure, query inefficiency, uso de cache e gargalos de I/O.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".sql", ".xml", ".yml", ".yaml", ".properties");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("pom.xml", "build.gradle", "application.yml", "application.properties");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return metadata.fileCount() > 0;
    }

    @Override
    public int executionOrder() {
        return 40;
    }
}

