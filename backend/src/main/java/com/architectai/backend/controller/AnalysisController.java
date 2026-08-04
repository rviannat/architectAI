package com.architectai.backend.controller;

import com.architectai.backend.dto.AnalysisRequest;
import com.architectai.backend.model.Analysis;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final Map<String, Analysis> analyses = new ConcurrentHashMap<>();

    @PostMapping("/projects/{projectId}/analyses")
    public ResponseEntity<Analysis> startAnalysis(@PathVariable String projectId, @RequestBody AnalysisRequest req) {
        String id = UUID.randomUUID().toString();
        Analysis a = new Analysis(id, projectId, req.type(), "PENDING", Instant.now().toString());
        analyses.put(id, a);
        // In a real implementation we would enqueue a job to run the pipeline
        return ResponseEntity.created(URI.create("/api/v1/analyses/" + id)).body(a);
    }

    @GetMapping("/analyses/{id}")
    public ResponseEntity<Analysis> getAnalysis(@PathVariable String id) {
        Analysis a = analyses.get(id);
        if (a == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(a);
    }

    @GetMapping("/analyses/{id}/report")
    public ResponseEntity<String> getReport(@PathVariable String id) {
        Analysis a = analyses.get(id);
        if (a == null) return ResponseEntity.notFound().build();
        // Placeholder: in real app would return PDF blob or presigned URL
        return ResponseEntity.ok("Report for analysis " + id + " is not yet generated in the MVP.");
    }

}

