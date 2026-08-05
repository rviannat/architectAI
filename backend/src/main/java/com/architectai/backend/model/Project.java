package com.architectai.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    private String id;

    @Column(nullable = false, length = 2048)
    private String repoUrl;

    @Column(nullable = false, length = 128)
    private String defaultBranch;

    @Column(nullable = false)
    private Instant createdAt;

    public Project() {
    }

    public Project(String id, String repoUrl, String defaultBranch) {
        this.id = id;
        this.repoUrl = repoUrl;
        this.defaultBranch = defaultBranch;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

