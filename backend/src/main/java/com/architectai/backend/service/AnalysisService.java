package com.architectai.backend.service;

import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Finding;
import com.architectai.backend.model.Project;
import com.architectai.backend.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalysisService {

    private final ProjectService projectService;
    private final AnalysisRepository analysisRepository;

    @Autowired
    public AnalysisService(ProjectService projectService, AnalysisRepository analysisRepository) {
        this.projectService = projectService;
        this.analysisRepository = analysisRepository;
    }

    /**
     * Cria uma nova análise e a enfileira para processamento
     */
    @Transactional
    public Analysis createAnalysis(String projectId, String type) {
        Project project = projectService.getProject(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found: " + projectId);
        }

        String analysisId = UUID.randomUUID().toString();
        Analysis analysis = new Analysis(analysisId, projectId, type);

        analysisRepository.save(analysis);
        
        return analysis;
    }

    /**
     * Obtém status de uma análise
     */
    @Transactional(readOnly = true)
    public Analysis getAnalysis(String analysisId) {
        return analysisRepository.findDetailedById(analysisId).orElse(null);
    }

    /**
     * Lista análises de um projeto
     */
    @Transactional(readOnly = true)
    public List<Analysis> listAnalysisByProject(String projectId) {
        return analysisRepository.findDetailedByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * Atualiza status de uma análise
     */
    @Transactional
    public void updateAnalysisStatus(String analysisId, String status) {
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis != null) {
            analysis.setStatus(status);
            if ("ANALYZING".equals(status)) {
                analysis.setStartedAt(LocalDateTime.now());
            } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                analysis.setFinishedAt(LocalDateTime.now());
            }
            analysisRepository.save(analysis);
        }
    }

    /**
     * Atualiza campos de análise após conclusão
     */
    @Transactional
    public void completeAnalysis(String analysisId, String repositoryPath, Integer findingsCount, String reportUrl, Long estimatedCostRs) {
        doCompleteAnalysis(analysisId, repositoryPath, findingsCount, reportUrl, "", "", estimatedCostRs, List.of(), Map.of(), null);
    }

    @Transactional
    public void completeAnalysis(
        String analysisId,
        String repositoryPath,
        Integer findingsCount,
        String reportUrl,
        String commercialReportUrl,
        String manifestUrl,
        Long estimatedCostRs
    ) {
        doCompleteAnalysis(analysisId, repositoryPath, findingsCount, reportUrl, commercialReportUrl, manifestUrl, estimatedCostRs, List.of(), Map.of(), null);
    }

    @Transactional
    public void completeAnalysis(
        String analysisId,
        String repositoryPath,
        Integer findingsCount,
        String reportUrl,
        String commercialReportUrl,
        String manifestUrl,
        Long estimatedCostRs,
        List<Finding> findings,
        Map<String, Integer> staticAnalysisFindingsByTool,
        String staticAnalysisReportUrl
    ) {
        doCompleteAnalysis(
            analysisId,
            repositoryPath,
            findingsCount,
            reportUrl,
            commercialReportUrl,
            manifestUrl,
            estimatedCostRs,
            findings,
            staticAnalysisFindingsByTool,
            staticAnalysisReportUrl
        );
    }

    private void doCompleteAnalysis(
        String analysisId,
        String repositoryPath,
        Integer findingsCount,
        String reportUrl,
        String commercialReportUrl,
        String manifestUrl,
        Long estimatedCostRs,
        List<Finding> findings,
        Map<String, Integer> staticAnalysisFindingsByTool,
        String staticAnalysisReportUrl
    ) {
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis != null) {
            analysis.setRepositoryPath(repositoryPath);
            analysis.setFindingsCount(findingsCount);
            analysis.setReportUrl(reportUrl);
            analysis.setCommercialReportUrl(commercialReportUrl);
            analysis.setManifestUrl(manifestUrl);
            analysis.setEstimatedCostRs(estimatedCostRs);
            analysis.setFindings(findings);
            analysis.setStaticAnalysisFindingsByTool(staticAnalysisFindingsByTool);
            analysis.setStaticAnalysisReportUrl(staticAnalysisReportUrl);
            analysis.setStatus("COMPLETED");
            analysis.setFinishedAt(LocalDateTime.now());
            analysisRepository.save(analysis);
        }
    }

    /**
     * Marca análise como falha
     */
    public void failAnalysis(String analysisId, String errorMessage) {
        Analysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis != null) {
            analysis.setStatus("FAILED");
            analysis.setErrorMessage(errorMessage);
            analysis.setFinishedAt(LocalDateTime.now());
            analysisRepository.save(analysis);
        }
    }

    /**
     * Retorna tamanho da fila (para monitoramento)
     */
    public int getQueueSize() {
        return (int) analysisRepository.countByStatus("PENDING");
    }

    public List<Analysis> listPendingAnalyses() {
        return analysisRepository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

}

