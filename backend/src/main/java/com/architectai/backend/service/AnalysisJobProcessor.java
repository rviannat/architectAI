package com.architectai.backend.service;

import com.architectai.backend.ai.AIOrchestrator;
import com.architectai.backend.ai.AgentResponse;
import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.SpecialistAgent;
import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Finding;
import com.architectai.backend.model.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;
    
/**
 * Processa análises enfileiradas em background.
 * Para MVP, usa polling de 5 segundos. Em produção, usar Kafka/RabbitMQ.
 */
@Slf4j
@Component
@EnableScheduling
public class AnalysisJobProcessor {

    private final AnalysisService analysisService;
    private final ProjectService projectService;
    private final GitService gitService;
    private final ReportService reportService;
    private final StaticAnalysisService staticAnalysisService;
    private final AIOrchestrator aiOrchestrator;
    private final List<SpecialistAgent> specialistAgents;

    @Autowired
    public AnalysisJobProcessor(
            AnalysisService analysisService,
            ProjectService projectService,
            GitService gitService,
            ReportService reportService,
            StaticAnalysisService staticAnalysisService,
            AIOrchestrator aiOrchestrator,
            List<SpecialistAgent> specialistAgents
    ) {
        this.analysisService = analysisService;
        this.projectService = projectService;
        this.gitService = gitService;
        this.reportService = reportService;
        this.staticAnalysisService = staticAnalysisService;
        this.aiOrchestrator = aiOrchestrator;
        this.specialistAgents = specialistAgents;
        
        // Registrar todos os agentes especializados
        registerAgents();
    }

    /**
     * Registra todos os agentes no orquestrador
     */
    private void registerAgents() {
        specialistAgents.forEach(aiOrchestrator::registerAgent);
        log.info("Registrados {} agentes especializados", specialistAgents.size());
    }

    /**
     * Poll da fila a cada 5 segundos e processa jobs
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void processAnalysisQueue() {
        List<Analysis> pendingAnalyses = analysisService.listPendingAnalyses();
        if (pendingAnalyses.isEmpty()) {
            return;
        }

        Analysis analysis = pendingAnalyses.get(0);

        log.info("Processando análise: {} para projeto: {}", analysis.getId(), analysis.getProjectId());
        processAnalysis(analysis);
    }

    /**
     * Orquestra o pipeline de análise completo com agentes IA
     */
    private void processAnalysis(Analysis analysis) {
        try {
            // Step 1: Atualizar status para CLONING
            analysisService.updateAnalysisStatus(analysis.getId(), "CLONING");
            
            log.info("Clonando repositório para análise: {}", analysis.getId());
            String repositoryPath = gitService.cloneRepositoryForAnalysis(analysis.getProjectId());
            
            if (repositoryPath == null || repositoryPath.isBlank()) {
                analysisService.failAnalysis(analysis.getId(), "Falha ao clonar repositório");
                return;
            }

            // Step 2: Extrair metadados do repositório
            log.info("Extraindo metadados do repositório: {}", analysis.getId());
            RepositoryMetadata metadata = extractRepositoryMetadata(repositoryPath);

            // Step 3: Atualizar status para ANALYZING
            analysisService.updateAnalysisStatus(analysis.getId(), "ANALYZING");

            // Step 4: Executar análise estática estruturada
            log.info("Executando análise estática para: {}", analysis.getId());
            StaticAnalysisResult staticAnalysisResult = staticAnalysisService.runStaticAnalysis(analysis.getId(), repositoryPath);

            List<Finding> staticFindings = staticAnalysisResult.getFindings();
            long staticFindingsCount = staticFindings.size();
            
            // Step 5: Executar análise com agentes IA
            log.info("Executando análise com {} agentes especializados", aiOrchestrator.getRegisteredAgents().size());
            Map<String, AgentResponse> agentResponses = aiOrchestrator.analyzeWithAgents(
                Path.of(repositoryPath),
                metadata,
                analysis.getId()
            );
            
            // Step 6: Consolidar responses (Tech Lead consolidation)
            AgentResponse techLeadResponse = aiOrchestrator.getTechLeadConsolidation(agentResponses);
            agentResponses.put(techLeadResponse.agentName(), techLeadResponse);
            
            // Step 7: Obter informações do projeto para gerar relatório
            Project project = projectService.getProject(analysis.getProjectId());
            
            // Step 8: Gerar relatório PDF profissional
            log.info("Gerando relatório PDF para análise: {}", analysis.getId());
            ReportService.ReportBundle reportBundle = reportService.generateReportBundle(analysis, project, agentResponses);
            
            // Step 9: Consolidar findings
            long totalFindings = agentResponses.values().stream()
                .mapToLong(r -> r.findings().size())
                .sum() + staticFindingsCount;
            
            // Step 10: Marcar análise como concluída
            analysisService.completeAnalysis(
                analysis.getId(),
                repositoryPath,
                (int) totalFindings,
                reportBundle.technicalMasterReportPath(),
                reportBundle.commercialMasterReportPath(),
                reportBundle.manifestPath(),
                estimateCost(totalFindings),
                staticFindings,
                staticAnalysisResult.getFindingsByTool(),
                staticAnalysisResult.getConsolidatedOutputPath()
            );
            
            log.info("Análise concluída com sucesso: {}", analysis.getId());

        } catch (Exception e) {
            log.error("Erro ao processar análise: {}", analysis.getId(), e);
            analysisService.failAnalysis(analysis.getId(), e.getMessage());
        }
    }

    /**
     * Extrai metadados do repositório para análise
     */
    private RepositoryMetadata extractRepositoryMetadata(String repositoryPath) {
        Path repoPath = Path.of(repositoryPath);
        Map<String, String> dependencies = new HashMap<>();

        long fileCount = 0;
        long totalLines = 0;
        boolean hasTests = false;
        boolean hasDocker = false;
        boolean hasKubernetes = false;
        boolean hasCiCd = false;
        boolean hasJava = false;
        boolean hasSql = false;
        boolean hasYaml = false;

        try (Stream<Path> files = Files.walk(repoPath)) {
            List<Path> collected = files.filter(Files::isRegularFile).toList();
            fileCount = collected.size();

            for (Path file : collected) {
                String normalizedPath = repoPath.relativize(file).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);

                if (normalizedPath.contains("/test/") || normalizedPath.startsWith("test/") || fileName.endsWith("test.java")) {
                    hasTests = true;
                }
                if (fileName.equals("dockerfile") || fileName.startsWith("docker-compose")) {
                    hasDocker = true;
                }
                if (normalizedPath.contains("k8s") || normalizedPath.contains("kubernetes") || fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                    hasYaml = true;
                }
                if (normalizedPath.contains(".github/workflows") || normalizedPath.contains("jenkinsfile") || normalizedPath.contains(".gitlab-ci")) {
                    hasCiCd = true;
                }
                if (fileName.endsWith(".java")) {
                    hasJava = true;
                }
                if (fileName.endsWith(".sql")) {
                    hasSql = true;
                }
                if (normalizedPath.contains("k8s") || normalizedPath.contains("kubernetes") || normalizedPath.contains("helm")) {
                    hasKubernetes = true;
                }

                if (fileName.equals("pom.xml") || fileName.equals("build.gradle") || fileName.equals("build.gradle.kts")) {
                    dependencies.put(fileName, "present");
                }

                try (Stream<String> lines = Files.lines(file)) {
                    totalLines += lines.count();
                } catch (Exception ignored) {
                    // Ignora arquivos binarios ou ilegiveis
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao extrair metadados do repositorio {}: {}", repositoryPath, e.getMessage());
        }

        String primaryLanguage = hasJava ? "java" : (hasSql ? "sql" : "unknown");
        List<String> languages = hasJava ? List.of("java") : List.of("unknown");
        List<String> frameworks = dependencies.containsKey("pom.xml") ? List.of("spring", "maven") : List.of();
        String databaseType = hasSql ? "sql" : "unknown";

        return new RepositoryMetadata(
            repositoryPath,
            primaryLanguage,
            languages,
            frameworks,
            dependencies,
            (int) fileCount,
            totalLines,
            hasTests,
            hasDocker,
            hasKubernetes || hasYaml,
            hasCiCd,
            databaseType,
            "unknown"
        );
    }

    /**
     * Estima o custo da auditoria baseado no número de findings
     */
    private Long estimateCost(long findingsCount) {
        // Algoritmo simples de estimativa
        if (findingsCount == 0) {
            return 2000L;  // Mínimo
        }
        
        long baseCost = 2000L;
        long perFinding = 50L;
        long estimatedCost = baseCost + (findingsCount * perFinding);
        
        return Math.min(estimatedCost, 15000L);  // Máximo
    }
}

