package com.architectai.backend.service;

import com.architectai.backend.config.StaticAnalysisProperties;
import com.architectai.backend.model.Finding;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PMDService {

    private static final Pattern TODO_PATTERN = Pattern.compile("(?i)(TODO|FIXME|XXX)");
    private static final Pattern SYSTEM_OUT_PATTERN = Pattern.compile("System\\.out\\.(print|println|printf)");
    private static final Pattern EMPTY_CATCH_PATTERN = Pattern.compile("catch\\s*\\(.*\\)\\s*\\{\\s*\\}");

    private final StaticAnalysisProperties properties;

    public PMDService(StaticAnalysisProperties properties) {
        this.properties = properties;
    }

    public List<Finding> analyze(Path repositoryRoot) {
        if (!properties.getTools().getPmd().isEnabled()) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();
        int maxFileLines = properties.getThresholds().getMaxFileLines();
        int maxMethodLines = properties.getThresholds().getMaxMethodLines();

        try {
            for (Path file : StaticAnalysisSupport.javaFiles(repositoryRoot)) {
                List<String> lines = StaticAnalysisSupport.readLines(file);
                String relative = StaticAnalysisSupport.relativePath(repositoryRoot, file);

                if (lines.size() > maxFileLines) {
                    findings.add(StaticAnalysisSupport.finding(
                        "PMD",
                        relative + ":file-too-large",
                        "CodeSize",
                        "MEDIUM",
                        relative,
                        1,
                        "Arquivo muito grande para manutenção eficiente.",
                        "lineCount=" + lines.size(),
                        "maintainability",
                        "file-size"
                    ));
                }

                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    String compact = line.replace('\t', ' ');
                    int lineNumber = index + 1;

                    if (TODO_PATTERN.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "PMD",
                            relative + ":" + lineNumber + ":todo",
                            "Documentation",
                            "LOW",
                            relative,
                            lineNumber,
                            "Comentário de pendência encontrado no código.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "documentation",
                            "todo"
                        ));
                    }

                    if (SYSTEM_OUT_PATTERN.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "PMD",
                            relative + ":" + lineNumber + ":system-out",
                            "CodeStyle",
                            "LOW",
                            relative,
                            lineNumber,
                            "Uso de System.out.* pode ser substituído por logging estruturado.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "code-style",
                            "logging"
                        ));
                    }

                    if (EMPTY_CATCH_PATTERN.matcher(compact).find()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "PMD",
                            relative + ":" + lineNumber + ":empty-catch",
                            "ErrorProne",
                            "MEDIUM",
                            relative,
                            lineNumber,
                            "Bloco catch vazio dificulta diagnóstico de falhas.",
                            StaticAnalysisSupport.truncate(compact, 140),
                            "reliability",
                            "exception-handling"
                        ));
                    }
                }

                findings.addAll(findLongMethods(repositoryRoot, file, relative, maxMethodLines));
            }
        } catch (Exception e) {
            findings.add(StaticAnalysisSupport.finding(
                "PMD",
                "scan-error",
                "ScanFailure",
                "INFO",
                repositoryRoot == null ? "unknown" : repositoryRoot.toString(),
                1,
                "Falha ao varrer o repositório para análise PMD-like.",
                e.getMessage(),
                "tooling"
            ));
        }

        return findings;
    }

    private List<Finding> findLongMethods(Path repositoryRoot, Path file, String relative, int maxMethodLines) throws Exception {
        List<String> lines = StaticAnalysisSupport.readLines(file);
        List<Finding> findings = new ArrayList<>();

        boolean inMethod = false;
        int methodStartLine = -1;
        int braceDepth = 0;
        String methodSignature = null;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String trimmed = line.trim();

            if (!inMethod && looksLikeMethodSignature(trimmed)) {
                inMethod = true;
                methodStartLine = index + 1;
                methodSignature = trimmed;
                braceDepth = countChar(trimmed, '{') - countChar(trimmed, '}');
                if (braceDepth <= 0) {
                    inMethod = false;
                    methodSignature = null;
                }
                continue;
            }

            if (!inMethod) {
                continue;
            }

            braceDepth += countChar(line, '{');
            braceDepth -= countChar(line, '}');

            if (braceDepth <= 0) {
                int methodLength = index + 1 - methodStartLine;
                if (methodLength > maxMethodLines) {
                    findings.add(StaticAnalysisSupport.finding(
                        "PMD",
                        relative + ":" + methodStartLine + ":long-method",
                        "MethodSize",
                        methodLength > maxMethodLines * 2 ? "HIGH" : "MEDIUM",
                        relative,
                        methodStartLine,
                        "Método longo demais para manutenção e revisão.",
                        StaticAnalysisSupport.truncate(methodSignature == null ? "" : methodSignature, 160) + " | lines=" + methodLength,
                        "maintainability",
                        "method-size"
                    ));
                }

                inMethod = false;
                methodSignature = null;
                methodStartLine = -1;
                braceDepth = 0;
            }
        }

        return findings;
    }

    private boolean looksLikeMethodSignature(String trimmedLine) {
        if (trimmedLine.isBlank() || trimmedLine.startsWith("//") || trimmedLine.startsWith("/*") || trimmedLine.startsWith("*")) {
            return false;
        }

        boolean hasParentheses = trimmedLine.contains("(") && trimmedLine.contains(")");
        boolean hasOpeningBrace = trimmedLine.contains("{");
        boolean looksLikeControlFlow = trimmedLine.startsWith("if ") || trimmedLine.startsWith("for ") || trimmedLine.startsWith("while ") || trimmedLine.startsWith("switch ") || trimmedLine.startsWith("catch ");
        boolean looksLikeDeclaration = trimmedLine.contains(" public ") || trimmedLine.startsWith("public ") || trimmedLine.startsWith("protected ") || trimmedLine.startsWith("private ") || trimmedLine.startsWith("static ");
        return hasParentheses && hasOpeningBrace && looksLikeDeclaration && !looksLikeControlFlow;
    }

    private int countChar(String value, char expected) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == expected) {
                count++;
            }
        }
        return count;
    }
}
