package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecuritySpecialistAgent extends AbstractLlmSpecialistAgent {

    public SecuritySpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Security Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em OWASP Top 10, autenticacao, autorizacao, segredos e criptografia.";
    }

    @Override
    protected String getAgentType() {
        return "SECURITY_SPECIALIST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Avalie OWASP Top 10, validacao de entrada, JWT/OAuth2, vazamento de segredo, autenticacao, autorizacao e headers de seguranca.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".kt", ".yml", ".yaml", ".properties", ".json", ".xml");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("application.yml", "application.yaml", "application.properties", "Dockerfile", ".env");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return metadata.fileCount() > 0;
    }

    @Override
    public int executionOrder() {
        return 30;
    }
}

