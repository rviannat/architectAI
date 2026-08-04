package com.architectai.backend.model;

import java.util.Date;

public class Project {
    private String id;
    private String repoUrl;
    private String defaultBranch;
    private Date createdAt;

    public Project() {
    }

    public Project(String id, String repoUrl, String defaultBranch) {
        this.id = id;
        this.repoUrl = repoUrl;
        this.defaultBranch = defaultBranch;
        this.createdAt = new Date();
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

