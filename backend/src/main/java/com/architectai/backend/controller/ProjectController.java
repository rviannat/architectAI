package com.architectai.backend.controller;

import com.architectai.backend.dto.ProjectRequest;
import com.architectai.backend.model.Project;
import com.architectai.backend.service.GitService;
import com.architectai.backend.service.ProjectService;
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

    // Use ProjectService as the single source of truth for projects
    private final ProjectService projectService;
    private final GitService gitService;

    public ProjectController(ProjectService projectService, GitService gitService) {
        this.projectService = projectService;
        this.gitService = gitService;
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody ProjectRequest req) {
        // Delegate creation to ProjectService so other services can access the project
        Project p = projectService.createProject(req.repoUrl(), req.defaultBranch());
        return ResponseEntity.created(URI.create("/api/v1/projects/" + p.getId())).body(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable String id) {
        Project p = projectService.getProject(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<String> cloneProject(@PathVariable String id, @RequestHeader(value = "X-Git-Token", required = false) String token) {
        Project p = projectService.getProject(id);
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
