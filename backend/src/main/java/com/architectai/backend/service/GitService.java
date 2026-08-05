package com.architectai.backend.service;

import com.architectai.backend.config.RuntimeProperties;
import com.architectai.backend.model.Project;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
    /**
     * Detecta o branch padrão do repositório remoto (HEAD).
     */
    public String detectDefaultBranch(String repoUrl, String token) {
        try {
            // setHeads(true) para listar refs/heads/* e detectar main/master
            LsRemoteCommand ls = Git.lsRemoteRepository().setRemote(repoUrl).setHeads(true).setTags(false);
            if (token != null && !token.isBlank()) {
                ls.setCredentialsProvider(new UsernamePasswordCredentialsProvider("x-access-token", token));
            }
            Collection<Ref> refs = ls.call();

            // Tenta detectar HEAD simbólico
            for (Ref ref : refs) {
                if ("HEAD".equals(ref.getName()) || ref.getName().endsWith("/HEAD")) {
                    String target = ref.getTarget() != null ? ref.getTarget().getName() : ref.getName();
                    if (target.startsWith("refs/heads/")) {
                        return target.substring("refs/heads/".length());
                    }
                }
            }

            // Prefere 'main', depois 'master', depois qualquer outro branch
            String firstBranch = null;
            for (Ref ref : refs) {
                String name = ref.getName();
                if (name.equals("refs/heads/main")) return "main";
                if (name.equals("refs/heads/master")) {
                    firstBranch = "master";
                } else if (firstBranch == null && name.startsWith("refs/heads/")) {
                    firstBranch = name.substring("refs/heads/".length());
                }
            }
            if (firstBranch != null) return firstBranch;

        } catch (Exception ignored) {}
        return "main"; // último fallback
    }

    public Path cloneRepository(String repoUrl, String branch, Path targetDir, String token) throws Exception {
        Files.createDirectories(targetDir);

        // Auto-detecta branch se não especificado ou se 'main' não existe
        String resolvedBranch = branch;
        if (resolvedBranch == null || resolvedBranch.isBlank()) {
            resolvedBranch = detectDefaultBranch(repoUrl, token);
        }

        CloneCommand cmd = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir.toFile())
                .setCloneAllBranches(false)
                .setDepth(1);

        if (resolvedBranch != null && !resolvedBranch.isBlank()) {
            cmd.setBranch(resolvedBranch);
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

        // Auto-detecta branch se não foi definido
        String branch = project.getDefaultBranch();
        if (branch == null || branch.isBlank()) {
            branch = detectDefaultBranch(project.getRepoUrl(), null);
        }

        Path clonedPath = cloneRepository(project.getRepoUrl(), branch, targetDir, null);
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

