package com.architectai.backend.service;

import com.architectai.backend.config.StaticAnalysisProperties;
import com.architectai.backend.model.Finding;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SpotBugsService {

    private static final Pattern OPTIONAL_GET = Pattern.compile("\\.get\\(\\)");
    private static final Pattern NULL_COMPARISON = Pattern.compile("==\\s*null|null\\s*==");
    private static final Pattern RETURN_NULL = Pattern.compile("return\\s+null\\s*;");
    private static final Pattern BROAD_CATCH = Pattern.compile("catch\\s*\\(\\s*(Exception|Throwable)");

    private final StaticAnalysisProperties properties;

    public SpotBugsService(StaticAnalysisProperties properties) {
        this.properties = properties;
    }

    public List<Finding> analyze(Path repositoryRoot) {
        if (!properties.getTools().getSpotbugs().isEnabled()) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();
        try {
            for (Path file : StaticAnalysisSupport.javaFiles(repositoryRoot)) {
                List<String> lines = StaticAnalysisSupport.readLines(file);
                String relative = StaticAnalysisSupport.relativePath(repositoryRoot, file);

                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    int lineNumber = index + 1;
                    String compact = line.replace('\t', ' ');

                    if (OPTIONAL_GET.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "SpotBugs",
                            relative + ":" + lineNumber + ":optional-get",
                            "NullPointerException",
                            "HIGH",
                            relative,
                            lineNumber,
                            "Possível desreferência de Optional.get() sem validação prévia.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "reliability",
                            "null-safety"
                        ));
                    }

                    if (NULL_COMPARISON.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "SpotBugs",
                            relative + ":" + lineNumber + ":null-comparison",
                            "NullPointerException",
                            "MEDIUM",
                            relative,
                            lineNumber,
                            "Comparação explícita com null pode mascarar fluxos frágeis de validação.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "reliability"
                        ));
                    }

                    if (RETURN_NULL.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "SpotBugs",
                            relative + ":" + lineNumber + ":return-null",
                            "APIContract",
                            "MEDIUM",
                            relative,
                            lineNumber,
                            "Retorno nulo explícito aumenta risco de NullPointerException nas chamadas seguintes.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "api-design",
                            "null-safety"
                        ));
                    }

                    if (BROAD_CATCH.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "SpotBugs",
                            relative + ":" + lineNumber + ":broad-catch",
                            "ExceptionHandling",
                            "MEDIUM",
                            relative,
                            lineNumber,
                            "Captura ampla de exceções pode ocultar falhas inesperadas durante a execução.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "reliability",
                            "exception-handling"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            findings.add(StaticAnalysisSupport.finding(
                "SpotBugs",
                "scan-error",
                "ScanFailure",
                "INFO",
                repositoryRoot == null ? "unknown" : repositoryRoot.toString(),
                1,
                "Falha ao varrer o repositório para análise SpotBugs-like.",
                e.getMessage(),
                "tooling"
            ));
        }

        return findings;
    }
}
