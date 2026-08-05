package com.architectai.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Finding {
    private String tool;
    private String id;
    private String type;
    private String severity;
    private String file;
    private Integer line;
    private String message;
    private String evidence;
    private List<String> tags = new ArrayList<>();

    public Finding() {
    }

    public Finding(String tool, String id, String type, String severity, String file, Integer line, String message, String evidence, List<String> tags) {
        this.tool = tool;
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.file = file;
        this.line = line;
        this.message = message;
        this.evidence = evidence;
        setTags(tags);
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}

