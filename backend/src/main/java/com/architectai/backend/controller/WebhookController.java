package com.architectai.backend.controller;

import com.architectai.backend.model.Analysis;
import com.architectai.backend.service.AnalysisService;
import com.architectai.backend.service.ProjectService;
import com.architectai.backend.util.WebhookSecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final ProjectService projectService;
    private final AnalysisService analysisService;

    @Value("${github.webhook.secret:dev-secret-change-in-production}")
    private String webhookSecret;

    @Autowired
    public WebhookController(ProjectService projectService, AnalysisService analysisService) {
        this.projectService = projectService;
        this.analysisService = analysisService;
    }

    /**
     * POST /api/v1/webhooks/github
     * Processa webhooks de push do GitHub
     * Valida assinatura HMAC e cria Analysis automaticamente
     */
    @PostMapping("/github")
    public ResponseEntity<?> githubWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        
        try {
            // Validar assinatura
            if (!WebhookSecurityUtil.isValidSignature(rawPayload, webhookSecret, signature)) {
                return ResponseEntity.status(400).body("Invalid or missing signature");
            }

            // Parse JSON payload (usando Jackson internamente)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);

            // Extrair informações do repositório
            Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
            if (repo == null) {
                return ResponseEntity.badRequest().body("Missing repository object in payload");
            }

            String cloneUrl = (String) repo.get("clone_url");
            if (cloneUrl == null) cloneUrl = (String) repo.get("git_url");
            if (cloneUrl == null) cloneUrl = (String) repo.get("ssh_url");
            if (cloneUrl == null) {
                return ResponseEntity.badRequest().body("Repository clone URL not found in payload");
            }

            String ref = (String) payload.get("ref");
            String branch = "main";
            if (ref != null && ref.contains("/")) {
                branch = ref.substring(ref.lastIndexOf('/') + 1);
            }

            // Criar ou atualizar projeto
            String projectId = projectService.createProject(cloneUrl, branch).getId();

            // Criar análise e enfileirar
            Analysis analysis = analysisService.createAnalysis(projectId, "CODE_REVIEW");

            return ResponseEntity.ok(new WebhookResponse(
                    "Webhook processed successfully",
                    projectId,
                    analysis.getId(),
                    analysis.getStatus()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Webhook processing failed: " + e.getMessage());
        }
    }

    /**
     * DTO simples para resposta do webhook
     */
    public static class WebhookResponse {
        public String message;
        public String projectId;
        public String analysisId;
        public String status;

        public WebhookResponse(String message, String projectId, String analysisId, String status) {
            this.message = message;
            this.projectId = projectId;
            this.analysisId = analysisId;
            this.status = status;
        }
    }
}

