package com.architectai.backend.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class StaticAnalysisService {

    private final StaticAnalysisExecutor staticAnalysisExecutor;

    public StaticAnalysisService(StaticAnalysisExecutor staticAnalysisExecutor) {
        this.staticAnalysisExecutor = staticAnalysisExecutor;
    }

    /**
     * Executa a análise estática estruturada da camada 3 e persiste os artefatos JSON.
     */
    public StaticAnalysisResult runStaticAnalysis(String analysisId, String repositoryPath) throws Exception {
        return staticAnalysisExecutor.execute(analysisId, Path.of(repositoryPath));
    }
}

