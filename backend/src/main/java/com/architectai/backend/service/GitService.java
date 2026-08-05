package com.architectai.backend.service;

import com.architectai.backend.config.RuntimeProperties;
import com.architectai.backend.model.Project;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class GitService {

    private final ProjectService projectService;
    private final RuntimeProperties runtimeProperties;

    @Autowired
    public GitService(ProjectService projectService, RuntimeProperties runtimeProperties) {
        this.projectService = projectService;
        this.runtimeProperties = runtimeProperties;
    }

    /**
     * Clona um repositório git para o diretório alvo.
     * Se token for fornecido, usa como senha (usuário "x-access-token").
     */
    public Path cloneRepository(String repoUrl, String branch, Path targetDir, String token) throws Exception {
        Files.createDirectories(targetDir);

        CloneCommand cmd = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir.toFile())
                .setCloneAllBranches(false)
                .setDepth(1);

        if (branch != null && !branch.isBlank()) {
            cmd.setBranch(branch);
        }

        if (token != null && !token.isBlank()) {
            // GitHub aceita token como senha com qualquer usuário
            cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider("x-access-token", token));
        }

        try (Git git = cmd.call()) {
            // success
            return targetDir;
        } catch (Exception e) {
            // cleanup partial clone
            try {
                File f = targetDir.toFile();
                if (f.exists()) {
                    deleteRecursively(f);
                }
            } catch (Exception ex) {
                // ignore
            }
            throw e;
        }
    }

    /**
     * Clona repositório baseado em Project ID
     * Usado pelo pipeline de análise
     */
    public String cloneRepositoryForAnalysis(String projectId) throws Exception {
        Project project = projectService.getProject(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found: " + projectId);
        }

        String workspaceId = projectId + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path targetDir = Path.of(runtimeProperties.getWorkspaceDir()).resolve(workspaceId);
        
        Path clonedPath = cloneRepository(project.getRepoUrl(), project.getDefaultBranch(), targetDir, null);
        return clonedPath.toAbsolutePath().toString();
    }

    private void deleteRecursively(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) deleteRecursively(f);
        }
        file.delete();
    }
}

