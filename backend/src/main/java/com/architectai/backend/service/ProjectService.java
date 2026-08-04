package com.architectai.backend.service;

import com.architectai.backend.model.Project;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProjectService {
    private final Map<String, Project> projectStore = new ConcurrentHashMap<>();

    /**
     * Cria um novo projeto
     */
    public Project createProject(String repoUrl, String defaultBranch) {
        String projectId = UUID.randomUUID().toString();
        Project project = new Project();
        project.setId(projectId);
        project.setRepoUrl(repoUrl);
        project.setDefaultBranch(defaultBranch != null ? defaultBranch : "main");
        project.setCreatedAt(new Date());
        
        projectStore.put(projectId, project);
        return project;
    }

    /**
     * Obtém um projeto por ID
     */
    public Project getProject(String projectId) {
        return projectStore.get(projectId);
    }

    /**
     * Lista todos os projetos
     */
    public List<Project> listProjects() {
        return new ArrayList<>(projectStore.values());
    }

    /**
     * Deleta um projeto
     */
    public void deleteProject(String projectId) {
        projectStore.remove(projectId);
    }
}

