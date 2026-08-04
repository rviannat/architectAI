package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DevOpsSpecialistAgent extends AbstractLlmSpecialistAgent {

    public DevOpsSpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "DevOps Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em Docker, Kubernetes, CI/CD, observabilidade e infraestrutura como codigo.";
    }

    @Override
    protected String getAgentType() {
        return "DEVOPS_SPECIALIST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Revise Dockerfile, manifests Kubernetes, pipelines CI/CD, strategy de deploy, observabilidade e seguranca de supply chain.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".yml", ".yaml", ".tf", ".sh", ".ps1", ".json");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("Dockerfile", "docker-compose.yml", "Jenkinsfile", ".gitlab-ci.yml");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return metadata.hasDocker() || metadata.hasKubernetes() || metadata.hasCiCd();
    }

    @Override
    public int executionOrder() {
        return 50;
    }
}

