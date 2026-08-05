package com.architectai.backend.service;

import com.architectai.backend.config.StaticAnalysisProperties;
import com.architectai.backend.model.Finding;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CheckstyleService {

    private static final Pattern WILDCARD_IMPORT = Pattern.compile("^\\s*import\\s+.+\\.\\*;\\s*$");
    private static final Pattern CLASS_DECLARATION = Pattern.compile("\\bclass\\s+([A-Za-z0-9_]+)");

    private final StaticAnalysisProperties properties;

    public CheckstyleService(StaticAnalysisProperties properties) {
        this.properties = properties;
    }

    public List<Finding> analyze(Path repositoryRoot) {
        if (!properties.getTools().getCheckstyle().isEnabled()) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();
        int maxLineLength = properties.getThresholds().getMaxLineLength();

        try {
            for (Path file : StaticAnalysisSupport.javaFiles(repositoryRoot)) {
                List<String> lines = StaticAnalysisSupport.readLines(file);
                String relative = StaticAnalysisSupport.relativePath(repositoryRoot, file);

                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    int lineNumber = index + 1;

                    if (line.endsWith(" ") || line.endsWith("\t")) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Checkstyle",
                            relative + ":" + lineNumber + ":trailing-whitespace",
                            "Whitespace",
                            "LOW",
                            relative,
                            lineNumber,
                            "Whitespace no final da linha viola convenções de estilo.",
                            StaticAnalysisSupport.truncate(line, 140),
                            "style",
                            "whitespace"
                        ));
                    }

                    if (line.contains("\t")) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Checkstyle",
                            relative + ":" + lineNumber + ":tab-character",
                            "Indentation",
                            "LOW",
                            relative,
                            lineNumber,
                            "Caracteres de tabulação devem ser evitados em favor de espaços.",
                            StaticAnalysisSupport.truncate(line, 140),
                            "style",
                            "indentation"
                        ));
                    }

                    if (line.length() > maxLineLength) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Checkstyle",
                            relative + ":" + lineNumber + ":line-length",
                            "LineLength",
                            "LOW",
                            relative,
                            lineNumber,
                            "Linha excede o limite configurado de comprimento.",
                            "length=" + line.length(),
                            "style",
                            "readability"
                        ));
                    }

                    if (WILDCARD_IMPORT.matcher(line).matches()) {
                        findings.add(StaticAnalysisSupport.finding(
                            "Checkstyle",
                            relative + ":" + lineNumber + ":wildcard-import",
                            "ImportOrder",
                            "MEDIUM",
                            relative,
                            lineNumber,
                            "Wildcard import reduz clareza e pode aumentar acoplamento.",
                            StaticAnalysisSupport.truncate(line, 140),
                            "style",
                            "imports"
                        ));
                    }

                    var classMatcher = CLASS_DECLARATION.matcher(line);
                    if (classMatcher.find()) {
                        String className = classMatcher.group(1);
                        if (!className.matches("[A-Z][A-Za-z0-9]*")) {
                            findings.add(StaticAnalysisSupport.finding(
                                "Checkstyle",
                                relative + ":" + lineNumber + ":class-name",
                                "NamingConvention",
                                "MEDIUM",
                                relative,
                                lineNumber,
                                "Nome de classe fora da convenção PascalCase.",
                                className,
                                "style",
                                "naming"
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            findings.add(StaticAnalysisSupport.finding(
                "Checkstyle",
                "scan-error",
                "ScanFailure",
                "INFO",
                repositoryRoot == null ? "unknown" : repositoryRoot.toString(),
                1,
                "Falha ao varrer o repositório para análise Checkstyle-like.",
                e.getMessage(),
                "tooling"
            ));
        }

        return findings;
    }
}
