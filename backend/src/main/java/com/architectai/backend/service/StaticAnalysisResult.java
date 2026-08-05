package com.architectai.backend.service;

import com.architectai.backend.model.Finding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaticAnalysisResult {
    private final int findingsCount;
    private final int rawFindingsCount;
    private final String spotbugsOutputPath;
    private final String pmdOutputPath;
    private final String checkstyleOutputPath;
    private final String semgrepOutputPath;
    private final String consolidatedOutputPath;
    private final Map<String, Integer> findingsByTool;
    private final List<Finding> findings;

    public StaticAnalysisResult(int findingsCount, String spotbugsOutputPath, String pmdOutputPath) {
        this(findingsCount, findingsCount, spotbugsOutputPath, pmdOutputPath, null, null, null, Map.of(), List.of());
    }

    public StaticAnalysisResult(
        int findingsCount,
        int rawFindingsCount,
        String spotbugsOutputPath,
        String pmdOutputPath,
        String checkstyleOutputPath,
        String semgrepOutputPath,
        String consolidatedOutputPath,
        Map<String, Integer> findingsByTool,
        List<Finding> findings
    ) {
        this.findingsCount = findingsCount;
        this.rawFindingsCount = rawFindingsCount;
        this.spotbugsOutputPath = spotbugsOutputPath;
        this.pmdOutputPath = pmdOutputPath;
        this.checkstyleOutputPath = checkstyleOutputPath;
        this.semgrepOutputPath = semgrepOutputPath;
        this.consolidatedOutputPath = consolidatedOutputPath;
        this.findingsByTool = findingsByTool == null ? Map.of() : new LinkedHashMap<>(findingsByTool);
        this.findings = findings == null ? List.of() : new ArrayList<>(findings);
    }

    public int getFindingsCount() {
        return findingsCount;
    }

    public int getRawFindingsCount() {
        return rawFindingsCount;
    }

    public String getSpotbugsOutputPath() {
        return spotbugsOutputPath;
    }

    public String getPmdOutputPath() {
        return pmdOutputPath;
    }

    public String getCheckstyleOutputPath() {
        return checkstyleOutputPath;
    }

    public String getSemgrepOutputPath() {
        return semgrepOutputPath;
    }

    public String getConsolidatedOutputPath() {
        return consolidatedOutputPath;
    }

    public Map<String, Integer> getFindingsByTool() {
        return new LinkedHashMap<>(findingsByTool);
    }

    public List<Finding> getFindings() {
        return new ArrayList<>(findings);
    }
}

