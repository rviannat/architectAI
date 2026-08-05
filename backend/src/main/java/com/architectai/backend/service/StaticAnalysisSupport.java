package com.architectai.backend.service;

import com.architectai.backend.model.Finding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StaticAnalysisSupport {

    private StaticAnalysisSupport() {
    }

    static List<Path> javaFiles(Path repositoryRoot) throws IOException {
        if (repositoryRoot == null || !Files.exists(repositoryRoot)) {
            return List.of();
        }

        try (var stream = Files.walk(repositoryRoot)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".java"))
                .sorted()
                .toList();
        }
    }

    static List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    static String relativePath(Path repositoryRoot, Path file) {
        try {
            return repositoryRoot.relativize(file).toString().replace('\\', '/');
        } catch (Exception e) {
            return file.toString().replace('\\', '/');
        }
    }

    static Finding finding(
        String tool,
        String id,
        String type,
        String severity,
        String file,
        Integer line,
        String message,
        String evidence,
        String... tags
    ) {
        List<String> tagList = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.isBlank()) {
                    tagList.add(tag);
                }
            }
        }
        return new Finding(tool, id, type, severity, file, line, message, evidence, tagList);
    }

    static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\t', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
