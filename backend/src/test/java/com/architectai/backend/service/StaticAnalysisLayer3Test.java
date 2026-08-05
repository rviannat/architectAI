package com.architectai.backend.service;

import com.architectai.backend.config.StaticAnalysisProperties;
import com.architectai.backend.model.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticAnalysisLayer3Test {

    @TempDir
    Path tempDir;

    @Test
    void spotbugsShouldDetectNullSafetyIssues() throws Exception {
        Path file = writeJava("""
            package sample;
            import java.util.Optional;
            public class SpotBugsSample {
                public String read(Optional<String> value) {
                    if (value == null) {
                        return null;
                    }
                    return value.get();
                }
            }
            """);

        SpotBugsService service = new SpotBugsService(new StaticAnalysisProperties());
        List<Finding> findings = service.analyze(tempDir);

        assertFalse(findings.isEmpty());
        assertTrue(findings.stream().anyMatch(f -> "SpotBugs".equals(f.getTool()) && "HIGH".equals(f.getSeverity())));
        assertTrue(findings.stream().anyMatch(f -> file.toString().replace('\\', '/').contains(f.getFile())));
    }

    @Test
    void pmdShouldDetectTodoAndLongMethod() throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("package sample;\n");
        body.append("public class PmdSample {\n");
        body.append("    public void longMethod() {\n");
        body.append("        // TODO refine this\n");
        for (int i = 0; i < 90; i++) {
            body.append("        System.out.println(").append(i).append(");\n");
        }
        body.append("    }\n");
        body.append("}\n");
        writeJava(body.toString());

        PMDService service = new PMDService(new StaticAnalysisProperties());
        List<Finding> findings = service.analyze(tempDir);

        assertTrue(findings.stream().anyMatch(f -> "PMD".equals(f.getTool()) && "Documentation".equals(f.getType())));
        assertTrue(findings.stream().anyMatch(f -> "PMD".equals(f.getTool()) && "MethodSize".equals(f.getType())));
    }

    @Test
    void checkstyleShouldDetectStyleViolations() throws Exception {
        writeJava("""
            package sample;
            import java.util.*;
            public class checkstyle_sample {
	        public void run() {
                    String line = "This line is intentionally made longer than one hundred and twenty characters to trigger the configured Checkstyle threshold without ambiguity.";
                }
            }
            """);

        CheckstyleService service = new CheckstyleService(new StaticAnalysisProperties());
        List<Finding> findings = service.analyze(tempDir);

        assertTrue(findings.stream().anyMatch(f -> "Checkstyle".equals(f.getTool()) && "LineLength".equals(f.getType())));
        assertTrue(findings.stream().anyMatch(f -> "Checkstyle".equals(f.getTool()) && "NamingConvention".equals(f.getType())));
        assertTrue(findings.stream().anyMatch(f -> "Checkstyle".equals(f.getTool()) && "ImportOrder".equals(f.getType())));
    }

    @Test
    void semgrepShouldDetectSecurityIssues() throws Exception {
        writeJava("""
            package sample;
            public class SemgrepSample {
                public void run(String userInput, String command) throws Exception {
                    String apiKey = "hardcoded-secret";
                    String sql = "SELECT * FROM users WHERE name = '" + userInput + "'";
                    Runtime.getRuntime().exec(command);
                }
            }
            """);

        SemgrepService service = new SemgrepService(new StaticAnalysisProperties());
        List<Finding> findings = service.analyze(tempDir);

        assertTrue(findings.stream().anyMatch(f -> "Semgrep".equals(f.getTool()) && "CRITICAL".equals(f.getSeverity())));
        assertTrue(findings.stream().anyMatch(f -> "SQLInjection".equals(f.getType())));
        assertTrue(findings.stream().anyMatch(f -> "CommandInjection".equals(f.getType())));
    }

    @Test
    void executorShouldConsolidateAndWriteJsonOutputs() throws Exception {
        writeJava("""
            package sample;
            public class ExecutorSample {
                public void run(String userInput) {
                    String password = "secret"; System.out.println("TODO");
                    String sql = "SELECT * FROM users WHERE name = '" + userInput + "'";
                }

                public String check(String input) {
                    if (input == null) {
                        return null;
                    }
                    return input;
                }
            }
            """);

        StaticAnalysisProperties properties = new StaticAnalysisProperties();
        StaticAnalysisExecutor executor = new StaticAnalysisExecutor(
            new SpotBugsService(properties),
            new PMDService(properties),
            new CheckstyleService(properties),
            new SemgrepService(properties),
            new FindingsConsolidationService(),
            new ObjectMapper()
        );

        StaticAnalysisResult result = executor.execute("analysis-123", tempDir);

        assertNotNull(result);
        assertTrue(result.getRawFindingsCount() >= result.getFindingsCount());
        assertFalse(result.getFindings().isEmpty());
        assertTrue(Files.exists(Path.of(result.getConsolidatedOutputPath())));
        assertTrue(Files.exists(Path.of(result.getSpotbugsOutputPath())));
        assertTrue(Files.exists(Path.of(result.getPmdOutputPath())));
        assertTrue(Files.exists(Path.of(result.getCheckstyleOutputPath())));
        assertTrue(Files.exists(Path.of(result.getSemgrepOutputPath())));
        assertTrue(result.getFindingsByTool().containsKey("Semgrep"));
        assertTrue(result.getFindingsByTool().containsKey("PMD"));
        assertTrue(result.getFindingsByTool().containsKey("SpotBugs"));
    }

    private Path writeJava(String content) throws Exception {
        Path file = tempDir.resolve("sample/Example.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
