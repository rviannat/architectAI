package com.architectai.backend.service;

import com.architectai.backend.ai.AgentResponse;
import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Project;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Serviço de geração de relatórios PDF profissionais usando iText
 */
@Slf4j
@Service
public class ReportService {

    private static final String REPORTS_DIR = "./.architectai/reports";
    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Gera relatório PDF profissional
     */
    public String generatePDFReport(Analysis analysis, Project project, Map<String, AgentResponse> agentResponses) {
        try {
            ensureReportDirectory();
            
            String filename = "report_" + analysis.getId() + "_" + System.currentTimeMillis() + ".pdf";
            Path outputPath = Paths.get(REPORTS_DIR, filename);
            
            PdfWriter writer = new PdfWriter(outputPath.toString());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Configurar margens
            document.setMargins(40, 40, 40, 40);
            
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontSmall = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
            
            // 1. CAPA
            addCoverPage(document, fontBold, fontNormal, project, analysis);
            document.add(new AreaBreak());
            
            // 2. SUMÁRIO EXECUTIVO
            addExecutiveSummary(document, fontBold, fontNormal, agentResponses);
            document.add(new AreaBreak());
            
            // 3. VISÃO GERAL DO SISTEMA
            addSystemOverview(document, fontBold, fontNormal, project);
            document.add(new AreaBreak());
            
            // 4. FINDINGS PRINCIPAIS
            addMainFindings(document, fontBold, fontNormal, fontSmall, agentResponses);
            document.add(new AreaBreak());
            
            // 5. ROADMAP PRIORIZADO
            addRoadmap(document, fontBold, fontNormal, agentResponses);
            document.add(new AreaBreak());
            
            // 6. RECOMENDAÇÕES DETALHADAS
            addDetailedRecommendations(document, fontBold, fontNormal, agentResponses);
            
            document.close();
            
            log.info("PDF relatório gerado: {}", outputPath);
            return outputPath.toString();
            
        } catch (Exception e) {
            log.error("Erro ao gerar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar relatório PDF: " + e.getMessage(), e);
        }
    }

    // ===== SEÇÕES DO PDF =====

    private void addCoverPage(Document document, PdfFont fontBold, PdfFont fontNormal, Project project, Analysis analysis) throws IOException {
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
            .setMarginBottom(60);
        document.add(subtitle);
        
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

    private void addExecutiveSummary(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, AgentResponse> agentResponses) {
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
        
        Paragraph summary = new Paragraph("Análise técnica completa realizada por " + agentResponses.size() + " especialistas em arquitetura, código, segurança e desempenho. O relatório consolida as principais descobertas e recomendações estratégicas para melhorar a qualidade, segurança e escalabilidade do software.")
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
        addTableRow(infoTable, "Status:", project.getStatus() != null ? project.getStatus() : "ATIVO");
        
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

    private void addRoadmap(Document document, PdfFont fontBold, PdfFont fontNormal, Map<String, AgentResponse> agentResponses) {
        Paragraph heading = new Paragraph("ROADMAP PRIORIZADO")
            .setFont(fontBold)
            .setFontSize(18)
            .setMarginBottom(20);
        document.add(heading);
        
        Paragraph phase1 = new Paragraph("Fase 1: Quick Wins (Próximas 2 semanas)")
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
        
        Paragraph phase2 = new Paragraph("Fase 2: Médio Prazo (1-2 meses)")
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
    }

    // ===== MÉTODOS AUXILIARES =====

    private void ensureReportDirectory() throws IOException {
        Files.createDirectories(Paths.get(REPORTS_DIR));
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
        keyCell.add(new Paragraph(key).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(11));
        keyCell.setPadding(10);
        keyCell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
        table.addCell(keyCell);
        
        Cell valueCell = new Cell();
        valueCell.add(new Paragraph(value).setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)).setFontSize(10));
        valueCell.setPadding(10);
        table.addCell(valueCell);
    }

    private Cell createMetricCell(String label, String value) throws IOException {
        Cell cell = new Cell();
        cell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
        cell.setPadding(15);
        cell.setTextAlignment(TextAlignment.CENTER);
        
        Paragraph p1 = new Paragraph(value)
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(16);
        Paragraph p2 = new Paragraph(label)
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
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
