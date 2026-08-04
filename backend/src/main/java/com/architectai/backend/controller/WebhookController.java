package com.architectai.backend.controller;

import com.architectai.backend.service.GitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final GitService gitService;

    public WebhookController(GitService gitService) {
        this.gitService = gitService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> githubWebhook(@RequestBody Map<String, Object> payload,
                                                @RequestHeader(value = "X-Git-Token", required = false) String token) {
        try {
            // payload structure from GitHub push event
            Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
            if (repo == null) return ResponseEntity.badRequest().body("Missing repository object in payload");

            String cloneUrl = (String) repo.get("clone_url");
            if (cloneUrl == null) cloneUrl = (String) repo.get("git_url");
            if (cloneUrl == null) cloneUrl = (String) repo.get("ssh_url");
            if (cloneUrl == null) return ResponseEntity.badRequest().body("Repository clone URL not found in payload");

            String ref = (String) payload.get("ref");
            String branch = null;
            if (ref != null && ref.contains("/")) {
                branch = ref.substring(ref.lastIndexOf('/') + 1);
            }

            Path workspace = Path.of("./workspace").resolve("webhook-" + System.currentTimeMillis());
            Path cloned = gitService.cloneRepository(cloneUrl, branch, workspace, token);
            return ResponseEntity.ok("Cloned to: " + cloned.toAbsolutePath().toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Webhook processing failed: " + e.getMessage());
        }
    }
}

