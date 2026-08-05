package com.architectai.backend.service;

import com.architectai.backend.model.Finding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FindingsConsolidationService {

    private static final List<String> SEVERITY_ORDER = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO");

    public List<Finding> consolidate(List<Finding> findings) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }

        Map<String, Finding> deduplicated = new LinkedHashMap<>();
        for (Finding finding : findings) {
            if (finding == null) {
                continue;
            }

            String key = buildKey(finding);
            Finding existing = deduplicated.get(key);
            if (existing == null) {
                deduplicated.put(key, copyOf(finding));
                continue;
            }

            merge(existing, finding);
        }

        return deduplicated.values().stream()
            .sorted(Comparator
                .comparingInt((Finding f) -> severityRank(normalizeSeverity(f.getSeverity())))
                .thenComparing(f -> safe(f.getFile()))
                .thenComparing(f -> f.getLine() == null ? Integer.MAX_VALUE : f.getLine()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private void merge(Finding target, Finding candidate) {
        if (severityRank(normalizeSeverity(candidate.getSeverity())) < severityRank(normalizeSeverity(target.getSeverity()))) {
            target.setSeverity(candidate.getSeverity());
            target.setTool(candidate.getTool());
            target.setId(candidate.getId());
            target.setType(candidate.getType());
            target.setMessage(candidate.getMessage());
            target.setEvidence(candidate.getEvidence());
        }

        Set<String> mergedTags = target.getTags().stream().collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        mergedTags.addAll(candidate.getTags());
        target.setTags(new ArrayList<>(mergedTags));
    }

    private Finding copyOf(Finding source) {
        return new Finding(
            source.getTool(),
            source.getId(),
            source.getType(),
            source.getSeverity(),
            source.getFile(),
            source.getLine(),
            source.getMessage(),
            source.getEvidence(),
            source.getTags()
        );
    }

    private String buildKey(Finding finding) {
        return safe(finding.getFile()) + ":" + (finding.getLine() == null ? "?" : finding.getLine());
    }

    private String normalizeSeverity(String severity) {
        return severity == null ? "INFO" : severity.toUpperCase(Locale.ROOT);
    }

    private int severityRank(String severity) {
        int index = SEVERITY_ORDER.indexOf(severity);
        return index >= 0 ? index : SEVERITY_ORDER.size();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

