package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.AIProvider;
import com.architectai.backend.ai.AgentResponse;
import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.SpecialistAgent;
import com.architectai.backend.ai.llm.LlmClient;
import com.architectai.backend.config.AgentAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractLlmSpecialistAgent implements SpecialistAgent {
    private static final Logger log = LoggerFactory.getLogger(AbstractLlmSpecialistAgent.class);
    private static final int MAX_FILES = 45;
    private static final int MAX_FILE_SIZE_BYTES = 160_000;
    private static final int MAX_CONTEXT_CHARS = 80_000;

    private final LlmClient llmClient;
    private final AgentAiProperties properties;
    private final AgentResponseParser parser;

    protected AbstractLlmSpecialistAgent(LlmClient llmClient, AgentAiProperties properties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.parser = new AgentResponseParser(objectMapper);
    }

    @Override
    public AgentResponse analyze(Path repositoryPath, RepositoryMetadata metadata, String analysisId) {
        long start = System.currentTimeMillis();
        try {
            List<Path> relevantFiles = collectRelevantFiles(repositoryPath);
            String context = buildContext(repositoryPath, relevantFiles);

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(analysisId, metadata, relevantFiles, context);
            AIProvider provider = resolveProvider();
            String rawResponse = llmClient.askForJson(provider, systemPrompt, userPrompt);

            long duration = System.currentTimeMillis() - start;
            Map<String, Object> responseMetadata = new HashMap<>();
            responseMetadata.put("provider", provider.getId());
            responseMetadata.put("files_analyzed", relevantFiles.size());
            responseMetadata.put("context_length", context.length());
            responseMetadata.put("analysis_id", analysisId);
            responseMetadata.put("eligible_files", toRelativeFileList(repositoryPath, relevantFiles));
            responseMetadata.put("agent_domain", getAgentDomain());

            AgentResponse parsed = parser.parse(rawResponse, getAgentName(), getAgentType(), duration, responseMetadata);
            return ensureNonEmptyFindings(parsed, toRelativeFileList(repositoryPath, relevantFiles));
        } catch (Exception e) {
            log.error("Erro no agente {}: {}", getAgentName(), e.getMessage(), e);
            return new AgentResponse(
                getAgentName(),
                getAgentType(),
                System.currentTimeMillis(),
                System.currentTimeMillis() - start,
                "FAILED",
                List.of(),
                "Erro ao executar agente: " + e.getMessage(),
                List.of("Reexecutar analise com logs detalhados e validar o contexto do repositorio."),
                Map.of("error", e.getClass().getSimpleName())
            );
        }
    }

    protected abstract String getAgentType();

    protected abstract String getSpecialtyInstructions();

    protected abstract List<String> supportedExtensions();

    protected List<String> supportedFileNames() {
        return List.of();
    }

    private AIProvider resolveProvider() {
        String configured = properties.getDefaultProvider();
        if (configured == null || configured.isBlank()) {
            return AIProvider.OPENAI;
        }

        for (AIProvider provider : AIProvider.values()) {
            if (provider.getId().equalsIgnoreCase(configured)) {
                return provider;
            }
        }
        return AIProvider.OPENAI;
    }

    private List<Path> collectRelevantFiles(Path repositoryPath) throws IOException {
        List<String> extensions = supportedExtensions().stream()
            .map(ext -> ext.toLowerCase(Locale.ROOT))
            .toList();
        List<String> names = supportedFileNames().stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .toList();

        try (Stream<Path> walk = Files.walk(repositoryPath)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(path -> isRelevant(path, extensions, names))
                .sorted(Comparator.comparing(Path::toString))
                .limit(MAX_FILES)
                .collect(Collectors.toList());
        }
    }

    private boolean isRelevant(Path path, List<String> extensions, List<String> names) {
        String normalized = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (names.contains(normalized)) {
            return true;
        }
        return extensions.stream().anyMatch(normalized::endsWith);
    }

    private String buildContext(Path repositoryPath, List<Path> relevantFiles) {
        StringBuilder context = new StringBuilder();

        for (Path file : relevantFiles) {
            if (context.length() >= MAX_CONTEXT_CHARS) {
                break;
            }
            try {
                long size = Files.size(file);
                if (size > MAX_FILE_SIZE_BYTES) {
                    continue;
                }
                String content = Files.readString(file);
                String relative = repositoryPath.relativize(file).toString();
                context.append("\n--- FILE: ").append(relative).append(" ---\n");
                context.append(content).append("\n");
            } catch (IOException ex) {
                log.debug("Falha ao ler arquivo {}: {}", file, ex.getMessage());
            }
        }

        if (context.length() > MAX_CONTEXT_CHARS) {
            return context.substring(0, MAX_CONTEXT_CHARS);
        }
        return context.toString();
    }

    private String buildSystemPrompt() {
        return """
            You are a senior enterprise software auditor.
            You MUST return only valid JSON with this exact schema:
            {
              \"summary\": \"string\",
              \"findings\": [
                {
                  \"id\": \"string\",
                  \"type\": \"ARCHITECTURE|CODE_QUALITY|SECURITY|PERFORMANCE|DEVOPS|DATABASE|JAVA\",
                  \"severity\": \"CRITICAL|HIGH|MEDIUM|LOW|INFO\",
                  \"file\": \"string\",
                  \"line\": 1,
                  \"title\": \"string\",
                  \"description\": \"string\",
                  \"recommendation\": \"string\",
                  \"estimated_effort_hours\": 1,
                  \"confidence\": 0.0,
                  \"tags\": [\"string\"],
                  \"references\": [\"string\"]
                }
              ],
              \"recommendations\": [\"string\"]
            }
            Never include markdown. Never include extra keys outside this schema.
            """;
    }

    private String buildUserPrompt(String analysisId, RepositoryMetadata metadata, List<Path> files, String context) {
        List<String> fileNames = new ArrayList<>();
        for (Path file : files) {
            fileNames.add(file.getFileName().toString());
        }

        return """
            ANALYSIS ID: %s
            AGENT: %s
            SPECIALTY: %s

            REPOSITORY METADATA:
            - Primary language: %s
            - Languages: %s
            - Frameworks: %s
            - Dependencies count: %d
            - File count: %d
            - Total lines: %d
            - Has tests: %s
            - Has docker: %s
            - Has kubernetes: %s
            - Has ci/cd: %s
            - Database type: %s
            - Message queue: %s

            RELEVANT FILES (%d): %s

            CODE CONTEXT:
            %s

            Focus only on your specialty. Add practical recommendations with business impact.
            When possible, quantify impact in cost reduction, risk reduction, and delivery speed.
            """.formatted(
            analysisId,
            getAgentName(),
            getSpecialtyInstructions(),
            metadata.primaryLanguage(),
            metadata.languages(),
            metadata.frameworks(),
            metadata.dependencies().size(),
            metadata.fileCount(),
            metadata.totalLines(),
            metadata.hasTests(),
            metadata.hasDocker(),
            metadata.hasKubernetes(),
            metadata.hasCiCd(),
            metadata.databaseType(),
            metadata.messageQueue(),
            files.size(),
            fileNames,
            context
        );
    }

    private List<String> toRelativeFileList(Path repositoryPath, List<Path> relevantFiles) {
        return relevantFiles.stream()
            .map(path -> repositoryPath.relativize(path).toString().replace('\\', '/'))
            .limit(20)
            .toList();
    }

    private AgentResponse ensureNonEmptyFindings(AgentResponse response, List<String> eligibleFiles) {
        if (!response.findings().isEmpty()) {
            return response;
        }

        List<AgentResponse.Finding> fallbackFindings = new ArrayList<>();
        int index = 1;
        for (String file : eligibleFiles.stream().limit(5).toList()) {
            fallbackFindings.add(new AgentResponse.Finding(
                "ELIG-" + index,
                "CODE_QUALITY",
                "INFO",
                file,
                null,
                "Arquivo elegivel para analise especializada",
                "Este arquivo foi classificado como relevante para o agente " + getAgentName() + ".",
                "Revisar este arquivo com foco em " + getSpecialtyInstructions(),
                2,
                0.6,
                List.of("eligible-file", getAgentType().toLowerCase(Locale.ROOT)),
                List.of()
            ));
            index++;
        }

        List<String> recommendations = new ArrayList<>(response.recommendations());
        if (recommendations.isEmpty()) {
            recommendations.add("Configurar API key do provider para ampliar profundidade da analise automatica.");
            recommendations.add("Executar revisao humana dos arquivos elegiveis listados neste relatorio.");
        }

        String summary = response.summary();
        if (summary == null || summary.isBlank()) {
            summary = "Analise concluida com fallback de priorizacao por arquivos elegiveis.";
        }

        return new AgentResponse(
            response.agentName(),
            response.agentType(),
            response.timestamp(),
            response.executionTimeMs(),
            "PARTIAL",
            fallbackFindings,
            summary,
            recommendations,
            response.metadata()
        );
    }
}

