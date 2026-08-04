package com.architectai.backend.controller;

import com.architectai.backend.dto.AnalysisRequest;
import com.architectai.backend.model.Analysis;
import com.architectai.backend.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Autowired
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * POST /api/v1/projects/{projectId}/analyses
     * Cria uma nova análise e a enfileira para processamento
     */
    @PostMapping("/projects/{projectId}/analyses")
    public ResponseEntity<Analysis> startAnalysis(@PathVariable String projectId, @RequestBody AnalysisRequest req) {
        try {
            Analysis analysis = analysisService.createAnalysis(projectId, req.type());
            return ResponseEntity.created(URI.create("/api/v1/analyses/" + analysis.getId())).body(analysis);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/v1/analyses/{id}
     * Obtém status e informações de uma análise
     */
    @GetMapping("/analyses/{id}")
    public ResponseEntity<Analysis> getAnalysis(@PathVariable String id) {
        Analysis analysis = analysisService.getAnalysis(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analysis);
    }

    /**
     * GET /api/v1/projects/{projectId}/analyses
     * Lista todas as análises de um projeto
     */
    @GetMapping("/projects/{projectId}/analyses")
    public ResponseEntity<List<Analysis>> listProjectAnalyses(@PathVariable String projectId) {
        List<Analysis> analyses = analysisService.listAnalysisByProject(projectId);
        return ResponseEntity.ok(analyses);
    }

    /**
     * GET /api/v1/analyses/{id}/report
     * Retorna URL ou conteúdo do relatório (placeholder)
     */
    @GetMapping("/analyses/{id}/report")
    public ResponseEntity<String> getReport(@PathVariable String id) {
        Analysis analysis = analysisService.getAnalysis(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        
        if ("COMPLETED".equals(analysis.getStatus())) {
            // Em produção, retornaria blob PDF ou presigned URL
            return ResponseEntity.ok("Report URL: " + analysis.getReportUrl());
        } else if ("FAILED".equals(analysis.getStatus())) {
            return ResponseEntity.status(400).body("Analysis failed: " + analysis.getErrorMessage());
        }
        
        return ResponseEntity.status(202).body("Analysis is still processing. Status: " + analysis.getStatus());
    }

    /**
     * GET /api/v1/queue/size
     * Retorna tamanho da fila (para monitoramento)
     */
    @GetMapping("/queue/size")
    public ResponseEntity<Integer> getQueueSize() {
        int size = analysisService.getQueueSize();
        return ResponseEntity.ok(size);
    }

}

