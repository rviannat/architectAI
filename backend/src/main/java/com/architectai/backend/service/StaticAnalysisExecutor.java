package com.architectai.backend.service;

import com.architectai.backend.model.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StaticAnalysisExecutor {

    private final SpotBugsService spotBugsService;
    private final PMDService pmdService;
    private final CheckstyleService checkstyleService;
    private final SemgrepService semgrepService;
    private final FindingsConsolidationService findingsConsolidationService;
    private final ObjectMapper objectMapper;

    public StaticAnalysisExecutor(
        SpotBugsService spotBugsService,
        PMDService pmdService,
        CheckstyleService checkstyleService,
        SemgrepService semgrepService,
        FindingsConsolidationService findingsConsolidationService,
        ObjectMapper objectMapper
    ) {
        this.spotBugsService = spotBugsService;
        this.pmdService = pmdService;
        this.checkstyleService = checkstyleService;
        this.semgrepService = semgrepService;
        this.findingsConsolidationService = findingsConsolidationService;
        this.objectMapper = objectMapper;
    }

    public StaticAnalysisResult execute(String analysisId, Path repositoryRoot) {
        try {
            Path outputDir = repositoryRoot.resolve(".architectai").resolve("analyses").resolve(analysisId);
            Files.createDirectories(outputDir);

            List<Finding> spotbugsFindings = spotBugsService.analyze(repositoryRoot);
            List<Finding> pmdFindings = pmdService.analyze(repositoryRoot);
            List<Finding> checkstyleFindings = checkstyleService.analyze(repositoryRoot);
            List<Finding> semgrepFindings = semgrepService.analyze(repositoryRoot);

            List<Finding> rawFindings = new ArrayList<>();
            rawFindings.addAll(spotbugsFindings);
            rawFindings.addAll(pmdFindings);
            rawFindings.addAll(checkstyleFindings);
            rawFindings.addAll(semgrepFindings);

            List<Finding> consolidatedFindings = findingsConsolidationService.consolidate(rawFindings);

            Path spotbugsOutput = outputDir.resolve("spotbugs.json");
            Path pmdOutput = outputDir.resolve("pmd.json");
            Path checkstyleOutput = outputDir.resolve("checkstyle.json");
            Path semgrepOutput = outputDir.resolve("semgrep.json");
            Path consolidatedOutput = outputDir.resolve("findings-consolidated.json");
            Path summaryOutput = outputDir.resolve("analysis-summary.json");

            writeJson(spotbugsOutput, spotbugsFindings);
            writeJson(pmdOutput, pmdFindings);
            writeJson(checkstyleOutput, checkstyleFindings);
            writeJson(semgrepOutput, semgrepFindings);
            writeJson(consolidatedOutput, consolidatedFindings);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("analysisId", analysisId);
            summary.put("rawFindingsCount", rawFindings.size());
            summary.put("consolidatedFindingsCount", consolidatedFindings.size());
            summary.put("findingsByTool", Map.of(
                "SpotBugs", spotbugsFindings.size(),
                "PMD", pmdFindings.size(),
                "Checkstyle", checkstyleFindings.size(),
                "Semgrep", semgrepFindings.size()
            ));
            summary.put("spotbugsOutputPath", spotbugsOutput.toAbsolutePath().normalize().toString());
            summary.put("pmdOutputPath", pmdOutput.toAbsolutePath().normalize().toString());
            summary.put("checkstyleOutputPath", checkstyleOutput.toAbsolutePath().normalize().toString());
            summary.put("semgrepOutputPath", semgrepOutput.toAbsolutePath().normalize().toString());
            summary.put("consolidatedOutputPath", consolidatedOutput.toAbsolutePath().normalize().toString());
            summary.put("findings", consolidatedFindings);
            writeJson(summaryOutput, summary);

            Map<String, Integer> findingsByTool = new LinkedHashMap<>();
            findingsByTool.put("SpotBugs", spotbugsFindings.size());
            findingsByTool.put("PMD", pmdFindings.size());
            findingsByTool.put("Checkstyle", checkstyleFindings.size());
            findingsByTool.put("Semgrep", semgrepFindings.size());

            log.info(
                "Análise estática concluída para {}: raw={}, consolidated={}",
                analysisId,
                rawFindings.size(),
                consolidatedFindings.size()
            );

            return new StaticAnalysisResult(
                consolidatedFindings.size(),
                rawFindings.size(),
                spotbugsOutput.toAbsolutePath().normalize().toString(),
                pmdOutput.toAbsolutePath().normalize().toString(),
                checkstyleOutput.toAbsolutePath().normalize().toString(),
                semgrepOutput.toAbsolutePath().normalize().toString(),
                summaryOutput.toAbsolutePath().normalize().toString(),
                findingsByTool,
                consolidatedFindings
            );
        } catch (Exception e) {
            log.error("Erro ao executar análise estática para {}", analysisId, e);
            throw new RuntimeException("Erro ao executar análise estática: " + e.getMessage(), e);
        }
    }

    private void writeJson(Path outputPath, Object value) throws Exception {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), value);
        if (Files.size(outputPath) == 0) {
            Files.writeString(outputPath, "[]", StandardCharsets.UTF_8);
        }
    }
}
