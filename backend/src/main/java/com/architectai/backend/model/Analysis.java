package com.architectai.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Analysis {
    private String id;
    private String projectId;
    private String type; // "CODE_REVIEW", "ARCHITECTURE", "PERFORMANCE", "SECURITY", etc.
    private String status; // PENDING, CLONING, ANALYZING, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String agentVersion;
    private String repositoryPath; // local path after cloning
    private String errorMessage; // if status = FAILED
    private Integer findingsCount;
    private String reportUrl; // URL to generated PDF
    private String commercialReportUrl; // URL to generated commercial PDF
    private String manifestUrl; // path to report manifest
    private Long estimatedCostRs; // estimated cost in R$
    private String staticAnalysisReportUrl;
    private Map<String, Integer> staticAnalysisFindingsByTool = new LinkedHashMap<>();
    private List<Finding> findings = new ArrayList<>();

    public Analysis() {}

    public Analysis(String id, String projectId, String type) {
        this.id = id;
        this.projectId = projectId;
        this.type = type;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.findingsCount = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String agentVersion) { this.agentVersion = agentVersion; }

    public String getRepositoryPath() { return repositoryPath; }
    public void setRepositoryPath(String repositoryPath) { this.repositoryPath = repositoryPath; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getFindingsCount() { return findingsCount; }
    public void setFindingsCount(Integer findingsCount) { this.findingsCount = findingsCount; }

    public String getReportUrl() { return reportUrl; }
    public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }

    public String getCommercialReportUrl() { return commercialReportUrl; }
    public void setCommercialReportUrl(String commercialReportUrl) { this.commercialReportUrl = commercialReportUrl; }

    public String getManifestUrl() { return manifestUrl; }
    public void setManifestUrl(String manifestUrl) { this.manifestUrl = manifestUrl; }

    public Long getEstimatedCostRs() { return estimatedCostRs; }
    public void setEstimatedCostRs(Long estimatedCostRs) { this.estimatedCostRs = estimatedCostRs; }

    public String getStaticAnalysisReportUrl() { return staticAnalysisReportUrl; }
    public void setStaticAnalysisReportUrl(String staticAnalysisReportUrl) { this.staticAnalysisReportUrl = staticAnalysisReportUrl; }

    public Map<String, Integer> getStaticAnalysisFindingsByTool() { return new LinkedHashMap<>(staticAnalysisFindingsByTool); }
    public void setStaticAnalysisFindingsByTool(Map<String, Integer> staticAnalysisFindingsByTool) {
        this.staticAnalysisFindingsByTool = staticAnalysisFindingsByTool == null ? new LinkedHashMap<>() : new LinkedHashMap<>(staticAnalysisFindingsByTool);
    }

    public List<Finding> getFindings() { return new ArrayList<>(findings); }
    public void setFindings(List<Finding> findings) { this.findings = findings == null ? new ArrayList<>() : new ArrayList<>(findings); }
}

