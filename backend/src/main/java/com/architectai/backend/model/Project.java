package com.architectai.backend.model;

public class Project {
    private String id;
    private String repoUrl;
    private String defaultBranch;

    public Project() {
    }

    public Project(String id, String repoUrl, String defaultBranch) {
        this.id = id;
        this.repoUrl = repoUrl;
        this.defaultBranch = defaultBranch;
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
}

