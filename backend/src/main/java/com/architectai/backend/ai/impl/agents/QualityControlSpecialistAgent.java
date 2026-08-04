package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QualityControlSpecialistAgent extends AbstractLlmSpecialistAgent {

    public QualityControlSpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        super(llmClient, properties, objectMapper);
    }

    @Override
    public String getAgentName() {
        return "Quality Control Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especialista em qualidade de codigo, legibilidade, duplicacao, testabilidade e padroes de manutencao.";
    }

    @Override
    protected String getAgentType() {
        return "QUALITY_CONTROL_SPECIALIST";
    }

    @Override
    protected String getSpecialtyInstructions() {
        return "Avalie clean code, complexidade ciclomatica, acoplamento, duplicacao, consistencia de padroes, tratamento de erros, organizacao de testes e qualidade geral de manutencao. Em cada finding, explicite impacto comercial em custo, risco de producao e atraso de entrega (time-to-market), com recomendacoes priorizadas por ROI tecnico.";
    }

    @Override
    protected List<String> supportedExtensions() {
        return List.of(".java", ".kt", ".xml", ".yml", ".yaml", ".properties", ".md", ".sql");
    }

    @Override
    protected List<String> supportedFileNames() {
        return List.of("pom.xml", "build.gradle", "build.gradle.kts", "checkstyle.xml", "pmd.xml", "spotbugs-exclude.xml");
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return metadata.fileCount() > 0;
    }

    @Override
    public int executionOrder() {
        return 25;
    }
}

