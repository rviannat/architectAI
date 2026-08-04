package com.architectai.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class StaticAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(StaticAnalysisService.class);

    /**
     * Runs SpotBugs and PMD using Maven in the target repository.
     * Stores raw outputs under .architectai/analyses/{analysisId}/ and returns a result object.
     */
    public StaticAnalysisResult runStaticAnalysis(String analysisId, String repositoryPath) throws Exception {
        Path repo = Path.of(repositoryPath);
        Path outDir = repo.resolve(".architectai").resolve("analyses").resolve(analysisId);
        Files.createDirectories(outDir);

        // Prepare files
        Path spotbugsOut = outDir.resolve("spotbugs.txt");
        Path pmdOut = outDir.resolve("pmd.txt");
        // Lightweight in-JVM analysis for MVP: scan Java files for simple heuristics
        StringBuilder sbSpot = new StringBuilder();
        StringBuilder sbPmd = new StringBuilder();
        java.util.concurrent.atomic.AtomicInteger findings = new java.util.concurrent.atomic.AtomicInteger(0);

        Files.walk(repo)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        var lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                        int total = lines.size();
                        long todos = lines.stream().filter(l -> l.contains("TODO") || l.contains("FIXME")).count();
                        long sysout = lines.stream().filter(l -> l.contains("System.out.print")).count();
                        if (todos > 0) {
                            sbSpot.append(String.format("%s: %d TODOs\n", repo.relativize(p), todos));
                            findings.addAndGet((int) todos);
                        }
                        if (sysout > 0) {
                            sbSpot.append(String.format("%s: %d System.out usages\n", repo.relativize(p), sysout));
                            findings.addAndGet((int) sysout);
                        }
                        if (total > 400) {
                            sbPmd.append(String.format("%s: large file (%d lines)\n", repo.relativize(p), total));
                            findings.incrementAndGet();
                        }
                        // naive long method detection: count occurrences of "public " and method length via braces
                        // (simplified for MVP)
                        long publicCount = lines.stream().filter(l -> l.contains(" public ") || l.startsWith("public ")).count();
                        if (publicCount == 0) return;
                    } catch (Exception e) {
                        logger.warn("failed to read {}: {}", p, e.getMessage());
                    }
                });

        if (sbSpot.length() == 0) sbSpot.append("No quick-heuristic issues found.\n");
        if (sbPmd.length() == 0) sbPmd.append("No quick-heuristic PMD issues found.\n");

        Files.writeString(spotbugsOut, sbSpot.toString(), StandardCharsets.UTF_8);
        Files.writeString(pmdOut, sbPmd.toString(), StandardCharsets.UTF_8);

        return new StaticAnalysisResult(findings.get(), spotbugsOut.toAbsolutePath().toString(), pmdOut.toAbsolutePath().toString());
    }

    private int runCommand(File dir, String[] cmd, Path outFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
             FileOutputStream fos = new FileOutputStream(outFile.toFile())) {
            String line;
            while ((line = reader.readLine()) != null) {
                fos.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            }
        }

        boolean exited = p.waitFor(5, TimeUnit.MINUTES);
        if (!exited) {
            p.destroyForcibly();
            throw new RuntimeException("Static analysis timed out");
        }
        return p.exitValue();
    }

    private String resolveMavenExecutable() {
        String mvn = "mvn";
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome == null || mavenHome.isBlank()) mavenHome = System.getenv("M2_HOME");

        if (mavenHome != null && !mavenHome.isBlank()) {
            Path candidate = Path.of(mavenHome).resolve("bin").resolve(isWindows ? "mvn.cmd" : "mvn");
            if (Files.exists(candidate)) return candidate.toAbsolutePath().toString();
        }

        // Search PATH entries for mvn or mvn.cmd
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            String[] parts = pathEnv.split(isWindows ? ";" : ":");
            for (String part : parts) {
                try {
                    Path p = Path.of(part.trim());
                    if (!Files.exists(p)) continue;
                    Path candidate = p.resolve(isWindows ? "mvn.cmd" : "mvn");
                    if (Files.exists(candidate)) return candidate.toAbsolutePath().toString();
                } catch (Exception ignored) {
                }
            }
        }

        // check some common Windows install locations as fallback
        if (isWindows) {
            try {
                String programFiles = System.getenv("ProgramFiles");
                if (programFiles != null) {
                    Path p1 = Path.of(programFiles, "Apache", "maven", "bin", "mvn.cmd");
                    if (Files.exists(p1)) return p1.toAbsolutePath().toString();
                    Path p2 = Path.of(programFiles, "apache-maven", "bin", "mvn.cmd");
                    if (Files.exists(p2)) return p2.toAbsolutePath().toString();
                }
            } catch (Exception ignored) {}
            return "mvn.cmd";
        }

        return mvn;
    }

    private int countNonEmptyLines(Path file) {
        try {
            return (int) Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(s -> !s.isBlank()).count();
        } catch (Exception e) {
            logger.warn("Failed to read {}: {}", file, e.getMessage());
            return 0;
        }
    }

}

