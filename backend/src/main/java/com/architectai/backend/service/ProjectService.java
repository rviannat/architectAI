package com.architectai.backend.service;

import com.architectai.backend.model.Project;
import com.architectai.backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.Instant;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Cria um novo projeto
     */
    @Transactional
    public Project createProject(String repoUrl, String defaultBranch) {
        String projectId = UUID.randomUUID().toString();
        Project project = new Project();
        project.setId(projectId);
        project.setRepoUrl(repoUrl);
        project.setDefaultBranch(defaultBranch != null ? defaultBranch : "main");
        project.setCreatedAt(Instant.now());

        return projectRepository.save(project);
    }

    /**
     * Obtém um projeto por ID
     */
    public Project getProject(String projectId) {
        return projectRepository.findById(projectId).orElse(null);
    }

    /**
     * Lista todos os projetos
     */
    public List<Project> listProjects() {
        return projectRepository.findAll();
    }

    /**
     * Deleta um projeto
     */
    @Transactional
    public void deleteProject(String projectId) {
        projectRepository.deleteById(projectId);
    }
}

