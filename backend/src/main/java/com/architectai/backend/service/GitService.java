package com.architectai.backend.service;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GitService {

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

    private void deleteRecursively(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) deleteRecursively(f);
        }
        file.delete();
    }
}

