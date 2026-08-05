package com.architectai.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "analyses")
public class Analysis {
    @Id
    private String id;

    @Column(nullable = false, length = 64)
    private String projectId;

    @Column(nullable = false, length = 64)
    private String type; // "CODE_REVIEW", "ARCHITECTURE", "PERFORMANCE", "SECURITY", etc.

    @Column(nullable = false, length = 32)
    private String status; // PENDING, CLONING, ANALYZING, COMPLETED, FAILED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @Column(length = 128)
    private String agentVersion;

    @Column(length = 2048)
    private String repositoryPath; // local path after cloning

    @Column(length = 4096)
    private String errorMessage; // if status = FAILED

    @Column
    private Integer findingsCount;

    @Column(length = 2048)
    private String reportUrl; // URL to generated PDF

    @Column(length = 2048)
    private String commercialReportUrl; // URL to generated commercial PDF

    @Column(length = 2048)
    private String manifestUrl; // path to report manifest

    @Column
    private Long estimatedCostRs; // estimated cost in R$

    @Column(length = 2048)
    private String staticAnalysisReportUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_static_findings_by_tool", joinColumns = @JoinColumn(name = "analysis_id"))
    @MapKeyColumn(name = "tool")
    @Column(name = "finding_count")
    private Map<String, Integer> staticAnalysisFindingsByTool = new LinkedHashMap<>();

    @JsonIgnoreProperties("analysis")
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
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

    /**
     * Atualiza a coleção gerenciada pelo Hibernate sem trocar a referência da lista.
     * Trocar a instância quebra o rastreamento de orphan removal do Hibernate.
     */
    public void setFindings(List<Finding> incoming) {
        this.findings.clear();
        if (incoming != null) {
            for (Finding f : incoming) {
                f.setAnalysis(this);
                this.findings.add(f);
            }
        }
    }
}

