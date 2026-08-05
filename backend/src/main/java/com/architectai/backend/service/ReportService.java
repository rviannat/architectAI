package com.architectai.backend.service;

import com.architectai.backend.config.RuntimeProperties;
import com.architectai.backend.ai.AgentResponse;
import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Project;
import com.architectai.backend.storage.StorageService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serviço de geração de relatórios PDF profissionais usando iText
 */
@Slf4j
@Service
public class ReportService {

    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final long MIN_PDF_SIZE_BYTES = 512;

    private final RuntimeProperties runtimeProperties;
    private final StorageService storageService;

    public ReportService(RuntimeProperties runtimeProperties, StorageService storageService) {
        this.runtimeProperties = runtimeProperties;
        this.storageService = storageService;
    }

    private record TocEntry(String title, int page) {}

    public record ReportBundle(
        String technicalMasterReportPath,
        String commercialMasterReportPath,
        String manifestPath,
        Map<String, String> agentReportPaths
    ) {}

    /**
     * Gera relatório PDF profissional
     */
    public String generatePDFReport(Analysis analysis, Project project, Map<String, AgentResponse> agentResponses) {
        return generateReportBundle(analysis, project, agentResponses).technicalMasterReportPath();
    }

    public ReportBundle generateReportBundle(Analysis analysis, Project project, Map<String, AgentResponse> agentResponses) {
        try {
            ensureReportDirectory();

            Path analysisDir = Paths.get(runtimeProperties.getReportsDir(), "analysis_" + analysis.getId());
            Files.createDirectories(analysisDir);

            Path unifiedReportPath = generateUnifiedReportPdf(analysisDir, analysis, project, agentResponses);
            Path manifestPath = writeUnifiedManifest(analysisDir, analysis, unifiedReportPath, agentResponses);

            String unifiedAbsolutePath = unifiedReportPath.toAbsolutePath().normalize().toString();
            String objectKey = "analysis_" + analysis.getId() + "/" + unifiedReportPath.getFileName();
            byte[] pdfBytes = Files.readAllBytes(unifiedReportPath);
            String storedReportUrl = storageService.upload(objectKey, pdfBytes, "application/pdf");

            Map<String, String> agentPaths = new LinkedHashMap<>();
            agentPaths.put("Unified Report", storedReportUrl);

            log.info("Relatorio unico gerado para analise {}. PDF: {}", analysis.getId(), unifiedAbsolutePath);
            return new ReportBundle(
                storedReportUrl,
                "",
                manifestPath.toAbsolutePath().normalize().toString(),
                agentPaths
            );
            
        } catch (Exception e) {
            log.error("Erro ao gerar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar relatório PDF: " + e.getMessage(), e);
        }
    }

    private Path generateUnifiedReportPdf(
        Path analysisDir,
        Analysis analysis,
        Project project,
        Map<String, AgentResponse> agentResponses
    ) throws IOException {
        String filename = "report_unified_" + analysis.getId() + "_" + System.currentTimeMillis() + ".pdf";
        Path outputPath = analysisDir.resolve(filename);
        Path tempOutputPath = outputPath.resolveSibling(outputPath.getFileName() + ".tmp");

        Map<String, AgentResponse> technicalResponses = new LinkedHashMap<>();
        Map<String, AgentResponse> commercialResponses = new LinkedHashMap<>();
        partitionResponsesByDomain(agentResponses, technicalResponses, commercialResponses);

        List<Map.Entry<String, AgentResponse>> orderedAgents = agentResponses.entrySet().stream()
            .sorted(Comparator
                .comparing((Map.Entry<String, AgentResponse> entry) -> isCommercialResponse(entry.getValue()))
                .thenComparing(Map.Entry::getKey))
            .toList();

        try (PdfWriter writer = new PdfWriter(tempOutputPath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(40, 40, 40, 40);
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            List<TocEntry> tocEntries = new ArrayList<>();

            addCoverPage(document, fontBold, fontNormal, project, analysis, "RELATORIO UNICO INTEGRADO");
            document.add(new AreaBreak());
            document.add(new AreaBreak());

            tocEntries.add(new TocEntry("Sumario Executivo Tecnico", pdf.getNumberOfPages()));
            addExecutiveSummary(document, fontBold, fontNormal, technicalResponses.isEmpty() ? agentResponses : technicalResponses, "TECHNICAL");
            document.add(new AreaBreak());

            if (!commercialResponses.isEmpty()) {
                tocEntries.add(new TocEntry("Sumario Executivo Comercial", pdf.getNumberOfPages()));
                addExecutiveSummary(document, fontBold, fontNormal, commercialResponses, "COMMERCIAL");
                document.add(new AreaBreak());
            }

            tocEntries.add(new TocEntry("Roadmap Priorizado", pdf.getNumberOfPages()));
            addRoadmap(document, fontBold, fontNormal, technicalResponses.isEmpty() ? agentResponses : technicalResponses, "TECHNICAL");
            document.add(new AreaBreak());

            if (!commercialResponses.isEmpty()) {
                tocEntries.add(new TocEntry("Plano Executivo Comercial", pdf.getNumberOfPages()));
                addCommercialProposalSection(document, fontBold, fontNormal, commercialResponses);
                document.add(new AreaBreak());
            }

            for (int i = 0; i < orderedAgents.size(); i++) {
                Map.Entry<String, AgentResponse> entry = orderedAgents.get(i);
                String domain = isCommercialResponse(entry.getValue()) ? "COMERCIAL" : "TECNICO";
                tocEntries.add(new TocEntry("Agente: " + entry.getKey() + " [" + domain + "]", pdf.getNumberOfPages()));
                addSingleAgentSection(document, fontBold, fontNormal, entry.getKey(), entry.getValue());
                if (i < orderedAgents.size() - 1) {
                    document.add(new AreaBreak());
                }
            }

            renderIndexPage(pdf, fontBold, fontNormal, tocEntries);
        }

        return finalizePdf(tempOutputPath, outputPath);
    }

    private void renderIndexPage(
        PdfDocument pdf,
        PdfFont fontBold,
        PdfFont fontNormal,
        List<TocEntry> tocEntries
    ) {
        PdfPage indexPage = pdf.getPage(2);
        Rectangle pageSize = indexPage.getPageSize();
        Rectangle area = new Rectangle(40, 40, pageSize.getWidth() - 80, pageSize.getHeight() - 80);

        try (Canvas canvas = new Canvas(indexPage, area)) {
            canvas.add(new Paragraph("INDICE").setFont(fontBold).setFontSize(20).setMarginBottom(16));
            canvas.add(new Paragraph("Navegacao do relatorio consolidado").setFont(fontNormal).setFontSize(10).setMarginBottom(12));

            for (TocEntry entry : tocEntries) {
                String line = buildTocLine(entry.title(), entry.page());
                canvas.add(new Paragraph(line)
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setMarginBottom(2));
            }
        }
    }

    private String buildTocLine(String title, int page) {
        String dots = ".".repeat(60);
        String base = title.length() > 52 ? title.substring(0, 52) + "..." : title;
        return base + " " + dots + " " + page;
    }

    private void addSingleAgentSection(
        Document document,
        PdfFont fontBold,
        PdfFont fontNormal,
        String agentName,
        AgentResponse agentResponse
    ) {
        String domain = isCommercialResponse(agentResponse) ? "COMERCIAL" : "TECNICO";
        document.add(new Paragraph(agentName + " - " + domain)
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(8));

        document.add(new Paragraph(Objects.toString(agentResponse.summary(), "Sem resumo retornado."))
            .setFont(fontNormal)
            .setFontSize(10)
            .setMarginBottom(12));

        document.add(new Paragraph("Arquivos elegiveis").setFont(fontBold).setFontSize(12));
        addEligibleFilesList(document, fontNormal, agentResponse);

        document.add(new Paragraph("\nFindings").setFont(fontBold).setFontSize(12));
        addAgentFindings(document, fontNormal, agentResponse);

        document.add(new Paragraph("\nRecomendacoes").setFont(fontBold).setFontSize(12));
        addAgentRecommendations(document, fontNormal, agentResponse);
    }

    private Path generateMasterReportPdf(
        Path analysisDir,
        Analysis analysis,
        Project project,
        Map<String, AgentResponse> agentResponses,
        Map<String, Path> perAgentReports,
        String reportTitle,
        String reportScope
    ) throws IOException {
        String suffix = "COMMERCIAL".equals(reportScope) ? "commercial" : "technical";
        String filename = "report_master_" + suffix + "_" + analysis.getId() + "_" + System.currentTimeMillis() + ".pdf";
        Path outputPath = analysisDir.resolve(filename);
        Path tempOutputPath = outputPath.resolveSibling(outputPath.getFileName() + ".tmp");

        try (PdfWriter writer = new PdfWriter(tempOutputPath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setMargins(40, 40, 40, 40);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontSmall = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            addCoverPage(document, fontBold, fontNormal, project, analysis, reportTitle);
            document.add(new AreaBreak());

            addExecutiveSummary(document, fontBold, fontNormal, agentResponses, reportScope);
            document.add(new AreaBreak());

            addGeneratedReportsSection(document, fontBold, fontNormal, perAgentReports, reportScope);
            document.add(new AreaBreak());

            addSystemOverview(document, fontBold, fontNormal, project);
            document.add(new AreaBreak());

            addMainFindings(document, fontBold, fontNormal, fontSmall, agentResponses);
            document.add(new AreaBreak());

            addRoadmap(document, fontBold, fontNormal, agentResponses, reportScope);
            document.add(new AreaBreak());

            if ("COMMERCIAL".equals(reportScope)) {
                addCommercialProposalSection(document, fontBold, fontNormal, agentResponses);
                document.add(new AreaBreak());
            }

            addDetailedRecommendations(document, fontBold, fontNormal, agentResponses);
        }

        return finalizePdf(tempOutputPath, outputPath);
    }

    private void partitionResponsesByDomain(
        Map<String, AgentResponse> source,
        Map<String, AgentResponse> technicalTarget,
        Map<String, AgentResponse> commercialTarget
    ) {
        for (Map.Entry<String, AgentResponse> entry : source.entrySet()) {
            if (isCommercialResponse(entry.getValue())) {
                commercialTarget.put(entry.getKey(), entry.getValue());
            } else {
                technicalTarget.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean isCommercialResponse(AgentResponse response) {
        Map<String, Object> metadata = response.metadata() == null ? Map.of() : response.metadata();
        Object domain = metadata.get("agent_domain");
        if (domain != null && "COMMERCIAL".equalsIgnoreCase(domain.toString())) {
            return true;
        }

        String type = response.agentType();
        if (type == null) {
            return false;
        }
        return type.startsWith("PROPOSAL_")
            || type.startsWith("PRICING_")
            || type.startsWith("COMMERCIAL_");
    }

    private Path writeUnifiedManifest(
        Path analysisDir,
        Analysis analysis,
        Path unifiedReportPath,
        Map<String, AgentResponse> agentResponses
    ) throws IOException {
        Path manifestPath = analysisDir.resolve("manifest_" + analysis.getId() + ".md");
        StringBuilder sb = new StringBuilder();
        sb.append("# Manifest de Relatorios\n\n");
        sb.append("Analise: ").append(analysis.getId()).append("\n");
        sb.append("Gerado em: ").append(LocalDateTime.now().format(PDF_DATE_FORMAT)).append("\n\n");
        sb.append("## Relatorio Unico\n");
        sb.append("- ").append(unifiedReportPath.toAbsolutePath()).append("\n\n");
        sb.append("## Agentes incluidos\n");
        for (String agent : agentResponses.keySet()) {
            sb.append("- ").append(agent).append("\n");
        }
        Files.writeString(manifestPath, sb.toString());
        return manifestPath;
    }

    private Path generateAgentReportPdf(
        Path analysisDir,
        Analysis analysis,
        Project project,
        String agentName,
        AgentResponse agentResponse
    ) throws IOException {
        String filename = "report_agent_" + sanitizeFileName(agentName) + "_" + analysis.getId() + ".pdf";
        Path outputPath = analysisDir.resolve(filename);
        Path tempOutputPath = outputPath.resolveSibling(outputPath.getFileName() + ".tmp");

        try (PdfWriter writer = new PdfWriter(tempOutputPath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.setMargins(40, 40, 40, 40);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            Paragraph title = new Paragraph("ARCHITECT AI - Relatorio por Especialista")
                .setFont(fontBold)
                .setFontSize(22)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            document.add(title);

            Paragraph agentTitle = new Paragraph(agentName)
                .setFont(fontBold)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(agentTitle);

            addInfoRow(document, fontBold, fontNormal, "Analise ID:", analysis.getId());
            addInfoRow(document, fontBold, fontNormal, "Repositorio:", project.getRepoUrl());
            addInfoRow(document, fontBold, fontNormal, "Tipo:", analysis.getType());
            addInfoRow(document, fontBold, fontNormal, "Status do agente:", agentResponse.status());
            addInfoRow(document, fontBold, fontNormal, "Execucao (ms):", String.valueOf(agentResponse.executionTimeMs()));

            document.add(new Paragraph("\nARQUIVOS MAIS ELEGIVEIS PARA ESTE AGENTE")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10));
            addEligibleFilesList(document, fontNormal, agentResponse);

            document.add(new Paragraph("\nFINDINGS DO ESPECIALISTA")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10));
            addAgentFindings(document, fontNormal, agentResponse);

            document.add(new Paragraph("\nRECOMENDACOES")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10));
            addAgentRecommendations(document, fontNormal, agentResponse);
        }

        return finalizePdf(tempOutputPath, outputPath);
    }

    private Path finalizePdf(Path tempOutputPath, Path outputPath) throws IOException {
        long generatedSize = Files.size(tempOutputPath);
        if (generatedSize <= MIN_PDF_SIZE_BYTES) {
            throw new IllegalStateException("PDF gerado com tamanho invalido: " + generatedSize + " bytes");
        }

        Files.move(tempOutputPath, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        validatePdfHeader(outputPath);
        return outputPath;
    }

    private void validatePdfHeader(Path outputPath) throws IOException {
        byte[] header = new byte[5];
        int read;
        try (InputStream in = Files.newInputStream(outputPath)) {
            read = in.read(header);
        }
        if (read < 5) {
            throw new IllegalStateException("PDF sem cabecalho valido: " + outputPath);
        }
        String signature = new String(header, 0, 5);
        if (!signature.startsWith("%PDF-")) {
            throw new IllegalStateException("Arquivo gerado nao eh PDF valido: " + outputPath);
        }
    }

    private void addGeneratedReportsSection(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, Path> perAgentReports, String reportScope) {
        Paragraph heading = new Paragraph("ARTEFATOS GERADOS")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);

        String scopeText = "COMMERCIAL".equals(reportScope)
            ? "Este pacote mostra o consolidado executivo/comercial e os anexos por agente."
            : "Este pacote mostra o consolidado tecnico e os anexos por agente.";

        Paragraph description = new Paragraph(scopeText)
            .setFont(fontNormal)
            .setFontSize(11)
            .setMarginBottom(10);
        document.add(description);

        for (Map.Entry<String, Path> entry : perAgentReports.entrySet()) {
            Paragraph item = new Paragraph("- " + entry.getKey() + ": " + entry.getValue().getFileName())
                .setFont(fontNormal)
                .setFontSize(10)
                .setMarginBottom(4);
            document.add(item);
        }
    }

    private void addEligibleFilesList(Document document, PdfFont fontNormal, AgentResponse agentResponse) {
        List<String> eligibleFiles = extractEligibleFiles(agentResponse);
        if (eligibleFiles.isEmpty()) {
            document.add(new Paragraph("Nenhum arquivo elegivel identificado para este agente nesta execucao.")
                .setFont(fontNormal)
                .setFontSize(10));
            return;
        }

        int idx = 1;
        for (String file : eligibleFiles.stream().limit(20).toList()) {
            document.add(new Paragraph(idx + ". " + file)
                .setFont(fontNormal)
                .setFontSize(10)
                .setMarginBottom(3));
            idx++;
        }
    }

    private void addAgentFindings(Document document, PdfFont fontNormal, AgentResponse agentResponse) {
        if (agentResponse.findings().isEmpty()) {
            document.add(new Paragraph("Nenhum finding estruturado retornado.")
                .setFont(fontNormal)
                .setFontSize(10));
            return;
        }

        int idx = 1;
        for (AgentResponse.Finding finding : agentResponse.findings().stream().limit(20).toList()) {
            String title = idx + ". [" + Objects.toString(finding.severity(), "INFO") + "] " + finding.title();
            document.add(new Paragraph(title).setBold().setFontSize(10));
            document.add(new Paragraph("Arquivo: " + Objects.toString(finding.file(), "unknown") + " | Linha: " + Objects.toString(finding.line(), "-"))
                .setFont(fontNormal)
                .setFontSize(9));
            document.add(new Paragraph("Descricao: " + Objects.toString(finding.description(), "Sem descricao"))
                .setFont(fontNormal)
                .setFontSize(9));
            document.add(new Paragraph("Recomendacao: " + Objects.toString(finding.recommendation(), "Sem recomendacao"))
                .setFont(fontNormal)
                .setFontSize(9)
                .setMarginBottom(6));
            idx++;
        }
    }

    private void addAgentRecommendations(Document document, PdfFont fontNormal, AgentResponse agentResponse) {
        if (agentResponse.recommendations().isEmpty()) {
            document.add(new Paragraph("Sem recomendacoes retornadas.")
                .setFont(fontNormal)
                .setFontSize(10));
            return;
        }

        for (String recommendation : agentResponse.recommendations().stream().limit(10).toList()) {
            document.add(new Paragraph("- " + recommendation)
                .setFont(fontNormal)
                .setFontSize(10)
                .setMarginBottom(4));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractEligibleFiles(AgentResponse agentResponse) {
        Map<String, Object> metadata = agentResponse.metadata();
        if (metadata == null) {
            return List.of();
        }

        Object value = metadata.get("eligible_files");
        if (value instanceof List<?> listValue) {
            List<String> files = new ArrayList<>();
            for (Object item : listValue) {
                if (item != null) {
                    files.add(item.toString());
                }
            }
            return files;
        }
        return List.of();
    }

    private String sanitizeFileName(String value) {
        return value.toLowerCase()
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_", "")
            .replaceAll("_$", "");
    }

    private Path writeManifest(Path analysisDir, Analysis analysis, Map<String, Path> perAgentReports, Path technicalMasterReport, Path commercialMasterReport) throws IOException {
        Path manifestPath = analysisDir.resolve("manifest_" + analysis.getId() + ".md");
        StringBuilder sb = new StringBuilder();
        sb.append("# Manifest de Relatorios\n\n");
        sb.append("Analise: ").append(analysis.getId()).append("\n");
        sb.append("Gerado em: ").append(LocalDateTime.now().format(PDF_DATE_FORMAT)).append("\n\n");
        sb.append("## Relatorio Geral Tecnico\n");
        sb.append("- ").append(technicalMasterReport.toAbsolutePath()).append("\n\n");

        sb.append("## Relatorio Geral Comercial\n");
        if (commercialMasterReport != null) {
            sb.append("- ").append(commercialMasterReport.toAbsolutePath()).append("\n\n");
        } else {
            sb.append("- Nao gerado (nenhum agente comercial executado)\n\n");
        }

        sb.append("## Relatorios por Agente\n");
        for (Map.Entry<String, Path> entry : perAgentReports.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().toAbsolutePath()).append("\n");
        }
        Files.writeString(manifestPath, sb.toString());
        return manifestPath;
    }

    // ===== SEÇÕES DO PDF =====

    private void addCoverPage(Document document, PdfFont fontBold, PdfFont fontNormal, Project project, Analysis analysis, String reportTitle) throws IOException {
        // Logo/Título
        Paragraph title = new Paragraph("ARCHITECT AI")
            .setFont(fontBold)
            .setFontSize(36)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(60);
        document.add(title);
        
        Paragraph subtitle = new Paragraph("AI-Powered Software Architecture & Engineering Consulting")
            .setFont(fontNormal)
            .setFontSize(12)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10);
        document.add(subtitle);

        Paragraph reportType = new Paragraph(reportTitle)
            .setFont(fontBold)
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(40);
        document.add(reportType);
        
        document.add(new LineSeparator(new SolidLine(1)));
        
        // Informações da Auditoria
        document.add(new Paragraph("\n"));
        addInfoRow(document, fontBold, fontNormal, "Tipo de Auditoria:", analysis.getType());
        addInfoRow(document, fontBold, fontNormal, "Repositório:", project.getRepoUrl());
        addInfoRow(document, fontBold, fontNormal, "Branch:", project.getDefaultBranch());
        addInfoRow(document, fontBold, fontNormal, "Data da Análise:", LocalDateTime.now().format(PDF_DATE_FORMAT));
        addInfoRow(document, fontBold, fontNormal, "ID da Análise:", analysis.getId());
        
        document.add(new Paragraph("\n\n"));
        document.add(new LineSeparator(new SolidLine(1)));
        
        Paragraph footer = new Paragraph("Relatório confidencial - Uso interno apenas")
            .setFont(fontNormal)
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(60);
        document.add(footer);
    }

    private void addExecutiveSummary(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, AgentResponse> agentResponses, String reportScope) {
        Paragraph heading = new Paragraph("SUMÁRIO EXECUTIVO")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);
        
        // Consolidar dados
        long totalFindings = agentResponses.values().stream()
            .mapToLong(r -> r.findings().size())
            .sum();
        
        long criticalCount = agentResponses.values().stream()
            .flatMap(r -> r.findings().stream())
            .filter(f -> "CRITICAL".equals(f.severity()))
            .count();
        
        long highCount = agentResponses.values().stream()
            .flatMap(r -> r.findings().stream())
            .filter(f -> "HIGH".equals(f.severity()))
            .count();
        
        // Sumário em caixas
        Table metricsTable = new Table(4);
        metricsTable.setWidth(UnitValue.createPercentValue(100));
        metricsTable.addCell(createMetricCell("Total de Achados", String.valueOf(totalFindings)));
        metricsTable.addCell(createMetricCell("Críticos", String.valueOf(criticalCount)));
        metricsTable.addCell(createMetricCell("Altos", String.valueOf(highCount)));
        metricsTable.addCell(createMetricCell("Especialistas", String.valueOf(agentResponses.size())));
        document.add(metricsTable);
        
        document.add(new Paragraph("\n"));
        
        String summaryText = "COMMERCIAL".equals(reportScope)
            ? "Analise executiva comercial realizada por agentes especializados para converter riscos tecnicos em oportunidade de receita, preservacao de margem e reducao de risco financeiro."
            : "Análise técnica completa realizada por " + agentResponses.size() + " especialistas em arquitetura, código, segurança e desempenho. O relatório consolida as principais descobertas e recomendações estratégicas para melhorar a qualidade, segurança e escalabilidade do software.";

        Paragraph summary = new Paragraph(summaryText)
            .setFont(fontNormal)
            .setFontSize(11)
            .setMarginBottom(20);
        document.add(summary);
    }

    private void addSystemOverview(Document document, PdfFont fontBold, PdfFont fontNormal, Project project) {
        Paragraph heading = new Paragraph("VISÃO GERAL DO SISTEMA")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);
        
        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));
        
        addTableRow(infoTable, "Repositório:", project.getRepoUrl());
        addTableRow(infoTable, "Branch Padrão:", project.getDefaultBranch());
        addTableRow(infoTable, "Linguagem Principal:", "Java");
        addTableRow(infoTable, "Frameworks Detectados:", "Spring Boot, JPA/Hibernate");
        addTableRow(infoTable, "Status:", "ATIVO");
        
        document.add(infoTable);
    }

    private void addMainFindings(Document document, PdfFont fontBold, PdfFont fontNormal, PdfFont fontSmall, Map<String, AgentResponse> agentResponses) {
        Paragraph heading = new Paragraph("PRINCIPAIS ACHADOS (TOP 20)")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);
        
        List<AgentResponse.Finding> allFindings = agentResponses.values().stream()
            .flatMap(r -> r.findings().stream())
            .sorted((a, b) -> {
                int severityCompare = severityToInt(b.severity()) - severityToInt(a.severity());
                if (severityCompare != 0) return severityCompare;
                return Double.compare(b.confidence() != null ? b.confidence() : 0, a.confidence() != null ? a.confidence() : 0);
            })
            .limit(20)
            .toList();

        if (allFindings.isEmpty()) {
            document.add(new Paragraph("Nenhum finding estruturado foi retornado pelos especialistas nesta execucao.")
                .setFont(fontNormal)
                .setFontSize(10));
            return;
        }
        
        int index = 1;
        for (AgentResponse.Finding finding : allFindings) {
            Paragraph findingTitle = new Paragraph(index + ". " + finding.title())
                .setFont(fontBold)
                .setFontSize(11)
                .setMarginTop(10)
                .setMarginBottom(5)
                .setFontColor(getSeverityColor(finding.severity()));
            document.add(findingTitle);
            
            Paragraph findingDesc = new Paragraph(finding.description())
                .setFont(fontNormal)
                .setFontSize(10)
                .setMarginBottom(3);
            document.add(findingDesc);
            
            Paragraph findingRec = new Paragraph("✓ " + finding.recommendation())
                .setFont(fontSmall)
                .setFontSize(9)
                .setMarginBottom(8)
                .setMarginLeft(10);
            document.add(findingRec);
            
            index++;
        }
    }

    private void addRoadmap(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, AgentResponse> agentResponses, String reportScope) {
        Paragraph heading = new Paragraph("ROADMAP PRIORIZADO")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);
        
        String phase1Title = "COMMERCIAL".equals(reportScope)
            ? "Fase 1: Conversao e Proposta (Proximas 2 semanas)"
            : "Fase 1: Quick Wins (Próximas 2 semanas)";

        Paragraph phase1 = new Paragraph(phase1Title)
            .setFont(fontBold)
            .setFontSize(12)
            .setMarginBottom(10);
        document.add(phase1);
        
        agentResponses.values().stream()
            .flatMap(r -> r.findings().stream())
            .filter(f -> "CRITICAL".equals(f.severity()) && (f.estimatedEffortHours() != null && f.estimatedEffortHours() <= 16))
            .limit(5)
            .forEach(f -> {
                Paragraph item = new Paragraph("• " + f.title() + " (" + f.estimatedEffortHours() + "h)")
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setMarginBottom(5)
                    .setMarginLeft(10);
                document.add(item);
            });
        
        document.add(new Paragraph("\n"));
        
        String phase2Title = "COMMERCIAL".equals(reportScope)
            ? "Fase 2: Expansao de Conta (30/60/90 dias)"
            : "Fase 2: Médio Prazo (1-2 meses)";

        Paragraph phase2 = new Paragraph(phase2Title)
            .setFont(fontBold)
            .setFontSize(12)
            .setMarginBottom(10);
        document.add(phase2);
        
        agentResponses.values().stream()
            .flatMap(r -> r.recommendations().stream())
            .distinct()
            .limit(5)
            .forEach(rec -> {
                Paragraph item = new Paragraph("• " + rec)
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setMarginBottom(5)
                    .setMarginLeft(10);
                document.add(item);
            });

        boolean hasRecommendations = agentResponses.values().stream().anyMatch(r -> !r.recommendations().isEmpty());
        if (!hasRecommendations) {
            document.add(new Paragraph("• Priorizar configuracao de provider de IA para aumentar a profundidade das recomendacoes.")
                .setFont(fontNormal)
                .setFontSize(10)
                .setMarginLeft(10));
        }
    }

    private void addCommercialProposalSection(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, AgentResponse> agentResponses) {
        Paragraph heading = new Paragraph("PLANO EXECUTIVO COMERCIAL")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);

        long criticalOrHigh = agentResponses.values().stream()
            .flatMap(r -> r.findings().stream())
            .filter(f -> "CRITICAL".equalsIgnoreCase(f.severity()) || "HIGH".equalsIgnoreCase(f.severity()))
            .count();

        long totalEffort = agentResponses.values().stream()
            .flatMap(r -> r.findings().stream())
            .map(AgentResponse.Finding::estimatedEffortHours)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum();

        Table proposalTable = new Table(2);
        proposalTable.setWidth(UnitValue.createPercentValue(100));
        addTableRow(proposalTable, "Risco tecnico prioritario:", criticalOrHigh + " itens de alto impacto");
        addTableRow(proposalTable, "Esforco consolidado estimado:", totalEffort + " horas");
        addTableRow(proposalTable, "Pacote sugerido:", "Auditoria + Plano de execucao 30/60/90 dias");
        addTableRow(proposalTable, "Modelo comercial:", "Projeto inicial + acompanhamento mensal");
        document.add(proposalTable);

        document.add(new Paragraph("\nAcoes comerciais recomendadas")
            .setFont(fontBold)
            .setFontSize(12)
            .setMarginTop(12)
            .setMarginBottom(8));

        document.add(new Paragraph("- Estruturar proposta em 3 pacotes (Essencial, Avancado, Enterprise) com precificacao por valor.")
            .setFont(fontNormal)
            .setFontSize(10)
            .setMarginBottom(4));
        document.add(new Paragraph("- Priorizar fechamento de quick wins para reduzir risco e provar ROI em ate 30 dias.")
            .setFont(fontNormal)
            .setFontSize(10)
            .setMarginBottom(4));
        document.add(new Paragraph("- Ofertar plano mensal de governanca tecnica/comercial para expansao de conta.")
            .setFont(fontNormal)
            .setFontSize(10)
            .setMarginBottom(4));

        document.add(new Paragraph("\nMATRIZ IMPACTO X RECEITA X ESFORCO")
            .setFont(fontBold)
            .setFontSize(12)
            .setMarginTop(12)
            .setMarginBottom(8));

        Table matrix = new Table(4);
        matrix.setWidth(UnitValue.createPercentValue(100));
        matrix.addHeaderCell(new Cell().add(new Paragraph("Prioridade").setBold().setFontSize(10)));
        matrix.addHeaderCell(new Cell().add(new Paragraph("Impacto").setBold().setFontSize(10)));
        matrix.addHeaderCell(new Cell().add(new Paragraph("Receita/Oportunidade").setBold().setFontSize(10)));
        matrix.addHeaderCell(new Cell().add(new Paragraph("Esforco").setBold().setFontSize(10)));

        matrix.addCell(new Cell().add(new Paragraph("P1").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Reducao imediata de risco operacional").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Aumenta confianca para fechar projeto inicial").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Baixo a medio").setFontSize(10)));

        matrix.addCell(new Cell().add(new Paragraph("P2").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Estabilizacao de qualidade e entrega").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Abre espaco para upsell de acompanhamento mensal").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Medio").setFontSize(10)));

        matrix.addCell(new Cell().add(new Paragraph("P3").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Escalabilidade e governanca de longo prazo").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Sustenta expansao de contrato Enterprise").setFontSize(10)));
        matrix.addCell(new Cell().add(new Paragraph("Medio a alto").setFontSize(10)));

        document.add(matrix);
    }

    private void addDetailedRecommendations(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, AgentResponse> agentResponses) {
        Paragraph heading = new Paragraph("RECOMENDAÇÕES DETALHADAS POR ESPECIALISTA")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);
        
        for (Map.Entry<String, AgentResponse> entry : agentResponses.entrySet()) {
            Paragraph agentName = new Paragraph("▸ " + entry.getKey())
                .setFont(fontBold)
                .setFontSize(12)
                .setMarginTop(15)
                .setMarginBottom(10);
            document.add(agentName);
            
            Paragraph agentSummary = new Paragraph(entry.getValue().summary())
                .setFont(fontNormal)
                .setFontSize(10)
                .setMarginBottom(8);
            document.add(agentSummary);
        }

        if (agentResponses.isEmpty()) {
            document.add(new Paragraph("Nenhuma resposta de agente foi registrada para esta analise.")
                .setFont(fontNormal)
                .setFontSize(10));
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    private void ensureReportDirectory() throws IOException {
        Files.createDirectories(Paths.get(runtimeProperties.getReportsDir()));
    }

    private void addInfoRow(Document document, PdfFont fontBold, PdfFont fontNormal, String label, String value) {
        Table row = new Table(2);
        row.setWidth(UnitValue.createPercentValue(100));
        
        Cell labelCell = new Cell();
        labelCell.add(new Paragraph(label).setFont(fontBold).setFontSize(11));
        labelCell.setWidth(UnitValue.createPercentValue(30));
        row.addCell(labelCell);
        
        Cell valueCell = new Cell();
        valueCell.add(new Paragraph(value).setFont(fontNormal).setFontSize(11));
        valueCell.setWidth(UnitValue.createPercentValue(70));
        row.addCell(valueCell);
        
        document.add(row);
    }

    private void addTableRow(Table table, String key, String value) {
        Cell keyCell = new Cell();
        keyCell.add(new Paragraph(key).setBold().setFontSize(11));
        keyCell.setPadding(10);
        keyCell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
        table.addCell(keyCell);
        
        Cell valueCell = new Cell();
        valueCell.add(new Paragraph(value).setFontSize(10));
        valueCell.setPadding(10);
        table.addCell(valueCell);
    }

    private Cell createMetricCell(String label, String value) {
        Cell cell = new Cell();
        cell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
        cell.setPadding(15);
        cell.setTextAlignment(TextAlignment.CENTER);
        
        Paragraph p1 = new Paragraph(value)
            .setBold()
            .setFontSize(16);
        Paragraph p2 = new Paragraph(label)
            .setFontSize(10);
        
        cell.add(p1);
        cell.add(p2);
        return cell;
    }

    private int severityToInt(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private com.itextpdf.kernel.colors.Color getSeverityColor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> com.itextpdf.kernel.colors.ColorConstants.RED;
            case "HIGH" -> new com.itextpdf.kernel.colors.DeviceRgb(255, 140, 0);  // Orange
            case "MEDIUM" -> new com.itextpdf.kernel.colors.DeviceRgb(255, 165, 0);  // Dark Orange
            case "LOW" -> com.itextpdf.kernel.colors.ColorConstants.BLUE;
            default -> com.itextpdf.kernel.colors.ColorConstants.BLACK;
        };
    }
}
