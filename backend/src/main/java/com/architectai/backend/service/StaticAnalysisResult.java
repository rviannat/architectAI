package com.architectai.backend.service;

public class StaticAnalysisResult {
    private final int findingsCount;
    private final String spotbugsOutputPath;
    private final String pmdOutputPath;

    public StaticAnalysisResult(int findingsCount, String spotbugsOutputPath, String pmdOutputPath) {
        this.findingsCount = findingsCount;
        this.spotbugsOutputPath = spotbugsOutputPath;
        this.pmdOutputPath = pmdOutputPath;
    }

    public int getFindingsCount() {
        return findingsCount;
    }

    public String getSpotbugsOutputPath() {
        return spotbugsOutputPath;
    }

    public String getPmdOutputPath() {
        return pmdOutputPath;
    }
}

