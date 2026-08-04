package com.architectai.backend.service;

import com.architectai.backend.ai.AIOrchestrator;
import com.architectai.backend.ai.AgentResponse;
import com.architectai.backend.ai.RepositoryMetadata;
import com.architectai.backend.ai.SpecialistAgent;
import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
    
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
    private final AIOrchestrator aiOrchestrator;
    private final List<SpecialistAgent> specialistAgents;

    @Autowired
    public AnalysisJobProcessor(
            AnalysisService analysisService,
            ProjectService projectService,
            GitService gitService,
            ReportService reportService,
            AIOrchestrator aiOrchestrator,
            List<SpecialistAgent> specialistAgents
    ) {
        this.analysisService = analysisService;
        this.projectService = projectService;
        this.gitService = gitService;
        this.reportService = reportService;
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
        if (AnalysisService.analysisQueue.isEmpty()) {
            return;
        }

        Analysis analysis = AnalysisService.analysisQueue.poll();
        if (analysis == null) {
            return;
        }

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
            
            // Step 4: Executar análise com agentes IA
            log.info("Executando análise com {} agentes especializados", aiOrchestrator.getRegisteredAgents().size());
            Map<String, AgentResponse> agentResponses = aiOrchestrator.analyzeWithAgents(
                Path.of(repositoryPath),
                metadata,
                analysis.getId()
            );
            
            // Step 5: Consolidar responses (Tech Lead consolidation)
            AgentResponse techLeadResponse = aiOrchestrator.getTechLeadConsolidation(agentResponses);
            
            // Step 6: Obter informações do projeto para gerar relatório
            Project project = projectService.getProject(analysis.getProjectId());
            
            // Step 7: Gerar relatório PDF profissional
            log.info("Gerando relatório PDF para análise: {}", analysis.getId());
            String reportUrl = reportService.generatePDFReport(analysis, project, agentResponses);
            
            // Step 8: Consolidar findings
            long totalFindings = agentResponses.values().stream()
                .mapToLong(r -> r.findings().size())
                .sum();
            
            // Step 9: Marcar análise como concluída
            analysisService.completeAnalysis(
                analysis.getId(),
                repositoryPath,
                (int) totalFindings,
                reportUrl,
                estimateCost(totalFindings)
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
        // Implementação simplificada para MVP
        // Em produção, adicionar detecção automática de linguagem, frameworks, etc
        
        return new RepositoryMetadata(
            "unknown",  // repositoryUrl
            "java",     // primaryLanguage
            List.of("java"),  // languages
            List.of("spring-boot", "spring-data-jpa"),  // frameworks
            Map.of(),   // dependencies
            0,          // fileCount
            0,          // totalLines
            true,       // hasTests
            false,      // hasDocker
            false,      // hasKubernetes
            false,      // hasCiCd
            "unknown",  // databaseType
            null        // messageQueue
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

