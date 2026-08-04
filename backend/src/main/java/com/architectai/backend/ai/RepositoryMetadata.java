package com.architectai.backend.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Metadados extraídos do repositório para análise
 */
public record RepositoryMetadata(
    @JsonProperty("repository_url")
    String repositoryUrl,

    @JsonProperty("primary_language")
    String primaryLanguage,  // java, python, go, typescript, etc

    @JsonProperty("languages")
    List<String> languages,

    @JsonProperty("frameworks")
    List<String> frameworks,  // spring, django, express, etc

    @JsonProperty("dependencies")
    Map<String, String> dependencies,

    @JsonProperty("file_count")
    int fileCount,

    @JsonProperty("total_lines")
    long totalLines,

    @JsonProperty("has_tests")
    boolean hasTests,

    @JsonProperty("has_docker")
    boolean hasDocker,

    @JsonProperty("has_kubernetes")
    boolean hasKubernetes,

    @JsonProperty("has_ci_cd")
    boolean hasCiCd,

    @JsonProperty("database_type")
    String databaseType,  // mysql, postgres, mongo, etc

    @JsonProperty("message_queue")
    String messageQueue  // kafka, rabbitmq, redis, etc
) {}

