package com.architectai.backend.ai.impl.agents;

import com.architectai.backend.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java Specialist Agent
 * Especializado em Java, Spring Boot, JPA, Hibernate, transações, concorrência, APIs REST
 */
@Slf4j
@Component
public class JavaSpecialistAgent implements SpecialistAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private static final int MAX_FILES_TO_ANALYZE = 50;
    private static final int MAX_FILE_SIZE_KB = 100;

    public JavaSpecialistAgent(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getAgentName() {
        return "Java Specialist";
    }

    @Override
    public String getAgentDescription() {
        return "Especializado em Java, Spring Boot, JPA, Hibernate, transações, concorrência, APIs REST e boas práticas do ecossistema Java";
    }

    @Override
    public AgentResponse analyze(Path repositoryPath, String analysisId) {
        try {
            log.info("Java Specialist iniciando análise de {}", repositoryPath);
            long start = System.currentTimeMillis();

            // Coletar arquivos Java relevantes
            List<String> javaFiles = collectJavaFiles(repositoryPath);
            if (javaFiles.isEmpty()) {
                log.warn("Nenhum arquivo Java encontrado");
                return createEmptyResponse(analysisId);
            }

            // Extrair código relevante para análise
            String codeContext = extractCodeContext(repositoryPath, javaFiles);

            // Chamar LLM com prompt especializado
            String prompt = buildJavaAnalysisPrompt(codeContext, javaFiles.size());
            String agentResponse = chatClient.prompt(prompt).call().content();

            // Parsear resposta e estruturar findings
            List<AgentResponse.Finding> findings = parseFindings(agentResponse, analysisId);

            long duration = System.currentTimeMillis() - start;
            return new AgentResponse(
                getAgentName(),
                "JAVA_SPECIALIST",
                System.currentTimeMillis(),
                duration,
                "SUCCESS",
                findings,
                generateSummary(findings),
                generateRecommendations(findings),
                Map.of(
                    "files_analyzed", javaFiles.size(),
                    "context_size", codeContext.length()
                )
            );

        } catch (Exception e) {
            log.error("Erro ao executar Java Specialist: {}", e.getMessage(), e);
            return createErrorResponse(analysisId, e.getMessage());
        }
    }

    @Override
    public boolean canHandle(RepositoryMetadata metadata) {
        return "java".equalsIgnoreCase(metadata.primaryLanguage()) ||
               metadata.languages().contains("java") ||
               metadata.frameworks().stream().anyMatch(f -> f.toLowerCase().contains("spring"));
    }

    @Override
    public int executionOrder() {
        return 10;  // Executa cedo, após identificação
    }

    // ===== MÉTODOS PRIVADOS =====

    private List<String> collectJavaFiles(Path repositoryPath) throws IOException {
        try (Stream<Path> walk = Files.walk(repositoryPath)) {
            return walk
                .filter(p -> p.toFile().isFile() && p.toString().endsWith(".java"))
                .map(Path::toString)
                .limit(MAX_FILES_TO_ANALYZE)
                .collect(Collectors.toList());
        }
    }

    private String extractCodeContext(Path repositoryPath, List<String> javaFiles) {
        StringBuilder context = new StringBuilder();
        
        for (String file : javaFiles.stream().limit(20).collect(Collectors.toList())) {
            try {
                Path filePath = Path.of(file);
                long sizeKB = Files.size(filePath) / 1024;
                
                if (sizeKB > MAX_FILE_SIZE_KB) {
                    continue;
                }
                
                String content = Files.readString(filePath);
                String relativePath = repositoryPath.relativize(filePath).toString();
                
                context.append("\n--- FILE: ").append(relativePath).append(" ---\n");
                context.append(content).append("\n");
                
                if (context.length() > 50000) {  // Limitar tamanho total
                    context.append("\n[... arquivo truncado por tamanho ...]\n");
                    break;
                }
            } catch (Exception e) {
                log.debug("Erro ao ler arquivo {}: {}", file, e.getMessage());
            }
        }
        
        return context.toString();
    }

    private String buildJavaAnalysisPrompt(String codeContext, int totalFiles) {
        return """
            Você é um especialista sênior em Java e Spring Boot com 15+ anos de experiência.
            
            Analise o seguinte código Java e identifique problemas em:
            1. **Padrões Spring Boot** - configuração, beans, injeção de dependências
            2. **JPA/Hibernate** - consultas N+1, lazy loading, transações
            3. **Java moderno** - uso de Optional, Streams, Records (Java 21+)
            4. **Concorrência** - thread safety, sincronização, pools
            5. **APIs REST** - versioning, erro handling, autenticação
            6. **Boas práticas** - separação de responsabilidades, testabilidade
            
            Total de arquivos Java encontrados: %d
            
            CÓDIGO A ANALISAR:
            %s
            
            RESPOSTA ESPERADA (JSON):
            {
              "findings": [
                {
                  "type": "SPRING_BOOT|JPA|CONCURRENCY|REST_API|CODE_QUALITY|PERFORMANCE",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "file": "package/ClassName.java",
                  "line": 42,
                  "title": "Breve título do problema",
                  "description": "Descrição detalhada do problema",
                  "recommendation": "Como corrigir",
                  "estimated_effort_hours": 4,
                  "confidence": 0.95,
                  "tags": ["spring-boot", "performance"]
                }
              ],
              "recommendations": [
                "Recomendação estratégica 1",
                "Recomendação estratégica 2"
              ]
            }
            
            Retorne APENAS JSON válido, sem explicações adicionais.
            """.formatted(totalFiles, codeContext);
    }

    private List<AgentResponse.Finding> parseFindings(String agentResponse, String analysisId) {
        List<AgentResponse.Finding> findings = new ArrayList<>();
        
        try {
            // Tentar parsear JSON da resposta
            String jsonStart = agentResponse.indexOf("{");
            String jsonEnd = agentResponse.lastIndexOf("}");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = agentResponse.substring(jsonStart, jsonEnd + 1);
                var parsed = objectMapper.readTree(jsonPart);
                
                if (parsed.has("findings")) {
                    parsed.get("findings").forEach(node -> {
                        try {
                            findings.add(objectMapper.treeToValue(node, AgentResponse.Finding.class));
                        } catch (Exception e) {
                            log.debug("Erro ao parsear finding: {}", e.getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao parsear resposta do LLM: {}", e.getMessage());
        }
        
        // Se não conseguiu parsear, criar findings genéricos
        if (findings.isEmpty()) {
            findings.add(new AgentResponse.Finding(
                "JAVA-001",
                "CODE_QUALITY",
                "MEDIUM",
                "unknown",
                null,
                "Análise incompleta",
                "Não foi possível extrair findings estruturados da análise",
                "Revise o código manualmente",
                4,
                0.5,
                List.of("java"),
                List.of()
            ));
        }
        
        return findings;
    }

    private String generateSummary(List<AgentResponse.Finding> findings) {
        long critical = findings.stream().filter(f -> "CRITICAL".equals(f.severity())).count();
        long high = findings.stream().filter(f -> "HIGH".equals(f.severity())).count();
        
        return String.format("Análise de código Java identificou %d problemas críticos e %d altos. Principais áreas: Spring Boot, JPA, concorrência.", critical, high);
    }

    private List<String> generateRecommendations(List<AgentResponse.Finding> findings) {
        List<String> recs = new ArrayList<>();
        
        boolean hasN1 = findings.stream().anyMatch(f -> f.description().toLowerCase().contains("n+1"));
        if (hasN1) {
            recs.add("Revisar queries JPA para evitar problema N+1 usando FETCH JOIN ou @EntityGraph");
        }
        
        boolean hasConcurrency = findings.stream().anyMatch(f -> "CONCURRENCY".equals(f.type()));
        if (hasConcurrency) {
            recs.add("Implementar mecanismos de sincronização e usar ExecutorService para thread pools");
        }
        
        boolean hasTransaction = findings.stream().anyMatch(f -> f.description().toLowerCase().contains("transaction"));
        if (hasTransaction) {
            recs.add("Revisar escopos de transação e usar @Transactional apropriadamente");
        }
        
        if (recs.isEmpty()) {
            recs.add("Revisão de arquitetura geral recomendada");
        }
        
        return recs;
    }

    private AgentResponse createEmptyResponse(String analysisId) {
        return new AgentResponse(
            getAgentName(),
            "JAVA_SPECIALIST",
            System.currentTimeMillis(),
            0,
            "SUCCESS",
            List.of(),
            "Nenhum arquivo Java encontrado no repositório",
            List.of(),
            Map.of()
        );
    }

    private AgentResponse createErrorResponse(String analysisId, String error) {
        return new AgentResponse(
            getAgentName(),
            "JAVA_SPECIALIST",
            System.currentTimeMillis(),
            0,
            "FAILED",
            List.of(),
            "Erro ao analisar: " + error,
            List.of(),
            Map.of("error", error)
        );
    }
}

