package com.architectai.backend.controller;

import com.architectai.backend.dto.ProjectRequest;
import com.architectai.backend.model.Project;
import com.architectai.backend.service.GitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final GitService gitService;

    public ProjectController(GitService gitService) {
        this.gitService = gitService;
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody ProjectRequest req) {
        String id = UUID.randomUUID().toString();
        Project p = new Project(id, req.repoUrl(), req.defaultBranch());
        projects.put(id, p);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + id)).body(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable String id) {
        Project p = projects.get(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<String> cloneProject(@PathVariable String id, @RequestHeader(value = "X-Git-Token", required = false) String token) {
        Project p = projects.get(id);
        if (p == null) return ResponseEntity.notFound().build();

        try {
            String branch = p.getDefaultBranch();
            Path workspace = Path.of("./workspace").resolve(id + "-" + System.currentTimeMillis());
            Path cloned = gitService.cloneRepository(p.getRepoUrl(), branch, workspace, token);
            return ResponseEntity.ok("Cloned to: " + cloned.toAbsolutePath().toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Clone failed: " + e.getMessage());
        }
    }

}
