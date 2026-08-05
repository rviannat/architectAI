package com.architectai.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "findings")
public class Finding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String persistenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column(nullable = false, length = 128)
    private String tool;
    @Column(nullable = false, length = 128)
    private String id;
    @Column(nullable = false, length = 128)
    private String type;
    @Column(nullable = false, length = 32)
    private String severity;
    @Column(length = 1024)
    private String file;
    @Column
    private Integer line;
    @Column(length = 4096)
    private String message;
    @Column(length = 8192)
    private String evidence;
    @Convert(converter = Finding.TagsConverter.class)
    @Column(name = "tags", length = 4096)
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

    public String getPersistenceId() {
        return persistenceId;
    }

    public void setPersistenceId(String persistenceId) {
        this.persistenceId = persistenceId;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    @Converter
    public static class TagsConverter implements AttributeConverter<List<String>, String> {
        private static final String DELIMITER = ",";

        @Override
        public String convertToDatabaseColumn(List<String> tags) {
            if (tags == null || tags.isEmpty()) return "";
            return String.join(DELIMITER, tags);
        }

        @Override
        public List<String> convertToEntityAttribute(String dbValue) {
            if (dbValue == null || dbValue.isBlank()) return new ArrayList<>();
            return new ArrayList<>(Arrays.asList(dbValue.split(DELIMITER)));
        }
    }
}

