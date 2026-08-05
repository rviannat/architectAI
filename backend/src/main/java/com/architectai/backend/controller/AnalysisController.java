package com.architectai.backend.controller;

import com.architectai.backend.dto.AnalysisCreateRequest;
import com.architectai.backend.dto.ApiResponse;
import com.architectai.backend.model.Analysis;
import com.architectai.backend.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;

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
    public ResponseEntity<ApiResponse<Analysis>> startAnalysis(@PathVariable String projectId, @Valid @RequestBody AnalysisCreateRequest req) {
        try {
            Analysis analysis = analysisService.createAnalysis(projectId, req.type());
            return ResponseEntity.created(URI.create("/api/v1/analyses/" + analysis.getId()))
                .body(ApiResponse.of(201, "Analysis created", analysis));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/v1/analyses/{id}
     * Obtém status e informações de uma análise
     */
    @GetMapping("/analyses/{id}")
    public ResponseEntity<ApiResponse<Analysis>> getAnalysis(@PathVariable String id) {
        Analysis analysis = analysisService.getAnalysis(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Analysis found", analysis));
    }

    /**
     * GET /api/v1/projects/{projectId}/analyses
     * Lista todas as análises de um projeto
     */
    @GetMapping("/projects/{projectId}/analyses")
    public ResponseEntity<ApiResponse<List<Analysis>>> listProjectAnalyses(@PathVariable String projectId) {
        List<Analysis> analyses = analysisService.listAnalysisByProject(projectId);
        return ResponseEntity.ok(ApiResponse.of(200, "Analyses listed", analyses));
    }

    /**
     * GET /api/v1/analyses/{id}/report
     * Retorna URL ou conteúdo do relatório (placeholder)
     */
    @GetMapping("/analyses/{id}/report")
    public ResponseEntity<ApiResponse<String>> getReport(@PathVariable String id) {
        Analysis analysis = analysisService.getAnalysis(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        
        if ("COMPLETED".equals(analysis.getStatus())) {
            // Em produção, retornaria blob PDF ou presigned URL
            return ResponseEntity.ok(ApiResponse.of(200, "Report available", analysis.getReportUrl()));
        } else if ("FAILED".equals(analysis.getStatus())) {
            return ResponseEntity.status(400).body(ApiResponse.of(400, "Analysis failed", analysis.getErrorMessage()));
        }
        
        return ResponseEntity.status(202).body(ApiResponse.of(202, "Analysis is still processing", analysis.getStatus()));
    }

    /**
     * GET /api/v1/analyses/{id}/reports
     * Retorna todos os artefatos de relatório gerados na análise.
     */
    @GetMapping("/analyses/{id}/reports")
    public ResponseEntity<ApiResponse<Map<String, String>>> getReports(@PathVariable String id) {
        Analysis analysis = analysisService.getAnalysis(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"COMPLETED".equals(analysis.getStatus())) {
            return ResponseEntity.status(202).body(ApiResponse.of(202, "Analysis is still processing", Map.of("status", analysis.getStatus())));
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("analysisId", analysis.getId());
        payload.put("technicalReport", analysis.getReportUrl());
        payload.put("commercialReport", analysis.getCommercialReportUrl());
        payload.put("manifest", analysis.getManifestUrl());
        return ResponseEntity.ok(ApiResponse.of(200, "Reports available", payload));
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

