package com.architectai.backend.model;

public class Analysis {
    private String id;
    private String projectId;
    private String type;
    private String status;
    private String startedAt;

    public Analysis() {}

    public Analysis(String id, String projectId, String type, String status, String startedAt) {
        this.id = id;
        this.projectId = projectId;
        this.type = type;
        this.status = status;
        this.startedAt = startedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }
}

