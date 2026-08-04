package com.architectai.backend.service;

import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class AnalysisService {

    private final Map<String, Analysis> analysisStore = new ConcurrentHashMap<>();
    public static final BlockingQueue<Analysis> analysisQueue = new LinkedBlockingQueue<>();
    
    private final ProjectService projectService;

    @Autowired
    public AnalysisService(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Cria uma nova análise e a enfileira para processamento
     */
    public Analysis createAnalysis(String projectId, String type) {
        Project project = projectService.getProject(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found: " + projectId);
        }

        String analysisId = UUID.randomUUID().toString();
        Analysis analysis = new Analysis(analysisId, projectId, type);
        
        analysisStore.put(analysisId, analysis);
        
        try {
            analysisQueue.put(analysis);
        } catch (InterruptedException e) {
            analysis.setStatus("FAILED");
            analysis.setErrorMessage("Failed to enqueue: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        return analysis;
    }

    /**
     * Obtém status de uma análise
     */
    public Analysis getAnalysis(String analysisId) {
        return analysisStore.getOrDefault(analysisId, null);
    }

    /**
     * Lista análises de um projeto
     */
    public List<Analysis> listAnalysisByProject(String projectId) {
        return analysisStore.values().stream()
                .filter(a -> a.getProjectId().equals(projectId))
                .toList();
    }

    /**
     * Atualiza status de uma análise
     */
    public void updateAnalysisStatus(String analysisId, String status) {
        Analysis analysis = analysisStore.get(analysisId);
        if (analysis != null) {
            analysis.setStatus(status);
            if ("ANALYZING".equals(status)) {
                analysis.setStartedAt(LocalDateTime.now());
            } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                analysis.setFinishedAt(LocalDateTime.now());
            }
        }
    }

    /**
     * Atualiza campos de análise após conclusão
     */
    public void completeAnalysis(String analysisId, String repositoryPath, Integer findingsCount, String reportUrl, Long estimatedCostRs) {
        Analysis analysis = analysisStore.get(analysisId);
        if (analysis != null) {
            analysis.setRepositoryPath(repositoryPath);
            analysis.setFindingsCount(findingsCount);
            analysis.setReportUrl(reportUrl);
            analysis.setEstimatedCostRs(estimatedCostRs);
            analysis.setStatus("COMPLETED");
            analysis.setFinishedAt(LocalDateTime.now());
        }
    }

    /**
     * Marca análise como falha
     */
    public void failAnalysis(String analysisId, String errorMessage) {
        Analysis analysis = analysisStore.get(analysisId);
        if (analysis != null) {
            analysis.setStatus("FAILED");
            analysis.setErrorMessage(errorMessage);
            analysis.setFinishedAt(LocalDateTime.now());
        }
    }

    /**
     * Retorna tamanho da fila (para monitoramento)
     */
    public int getQueueSize() {
        return analysisQueue.size();
    }

}

