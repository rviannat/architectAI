package com.architectai.backend.service;

import com.architectai.backend.config.StaticAnalysisProperties;
import com.architectai.backend.model.Finding;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SemgrepService {

    private static final Pattern HARD_CODED_SECRET = Pattern.compile("(?i).*(password|secret|token|api[_-]?key)\\s*=\\s*\"[^\"]+\".*");
    private static final Pattern SQL_CONCAT = Pattern.compile("(?i).*(select|insert|update|delete).*[+].*");
    private static final Pattern COMMAND_EXECUTION = Pattern.compile(".*(Runtime\\.getRuntime\\(\\)\\.exec|new\\s+ProcessBuilder).*", Pattern.CASE_INSENSITIVE);

    private final StaticAnalysisProperties properties;

    public SemgrepService(StaticAnalysisProperties properties) {
        this.properties = properties;
    }

    public List<Finding> analyze(Path repositoryRoot) {
        if (!properties.getTools().getSemgrep().isEnabled()) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();

        try {
            for (Path file : StaticAnalysisSupport.javaFiles(repositoryRoot)) {
                List<String> lines = StaticAnalysisSupport.readLines(file);
                String relative = StaticAnalysisSupport.relativePath(repositoryRoot, file);

                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    String compact = line.replace('\t', ' ');
                    int lineNumber = index + 1;

                    if (HARD_CODED_SECRET.matcher(compact).matches()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Semgrep",
                            relative + ":" + lineNumber + ":hardcoded-secret",
                            "CredentialExposure",
                            "CRITICAL",
                            relative,
                            lineNumber,
                            "Segredo ou credencial codificada diretamente no código.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "security",
                            "secrets"
                        ));
                    }

                    if (SQL_CONCAT.matcher(compact).matches()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Semgrep",
                            relative + ":" + lineNumber + ":sql-concat",
                            "SQLInjection",
                            "HIGH",
                            relative,
                            lineNumber,
                            "Concatenação de string em contexto SQL pode permitir injection.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "security",
                            "injection"
                        ));
                    }

                    if (COMMAND_EXECUTION.matcher(compact).matches()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Semgrep",
                            relative + ":" + lineNumber + ":command-execution",
                            "CommandInjection",
                            "HIGH",
                            relative,
                            lineNumber,
                            "Execução de comandos externos exige validação rigorosa dos parâmetros.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "security",
                            "command-injection"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            findings.add(StaticAnalysisSupport.finding(
                "Semgrep",
                "scan-error",
                "ScanFailure",
                "INFO",
                repositoryRoot == null ? "unknown" : repositoryRoot.toString(),
                1,
                "Falha ao varrer o repositório para análise Semgrep-like.",
                e.getMessage(),
                "tooling"
            ));
        }

        return findings;
    }
}
