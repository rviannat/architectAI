package com.architectai.backend.service;

import com.architectai.backend.model.Finding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindingsConsolidationServiceTest {

    private final FindingsConsolidationService service = new FindingsConsolidationService();

    @Test
    void consolidateShouldDeduplicateByFileAndLine() {
        List<Finding> findings = List.of(
            new Finding("SpotBugs", "BUG-1", "NullPointer", "HIGH", "src/A.java", 10, "First", "obj.a()", List.of("reliability", "null")),
            new Finding("PMD", "BUG-2", "NullPointer", "CRITICAL", "src/A.java", 10, "Second", "obj.b()", List.of("security")),
            new Finding("Checkstyle", "BUG-3", "Style", "LOW", "src/B.java", 4, "Third", "foo", List.of("style"))
        );

        List<Finding> consolidated = service.consolidate(findings);

        assertEquals(2, consolidated.size());
        assertEquals("CRITICAL", consolidated.get(0).getSeverity());
        assertEquals("src/A.java", consolidated.get(0).getFile());
        assertEquals(10, consolidated.get(0).getLine());
        assertTrue(consolidated.get(0).getTags().containsAll(List.of("reliability", "null", "security")));
    }

    @Test
    void consolidateShouldSortBySeverityAndLocation() {
        List<Finding> findings = List.of(
            new Finding("ToolA", "1", "Issue", "LOW", "b/File.java", 9, "Low", "e1", List.of()),
            new Finding("ToolB", "2", "Issue", "HIGH", "a/File.java", 2, "High", "e2", List.of()),
            new Finding("ToolC", "3", "Issue", "MEDIUM", "c/File.java", 1, "Medium", "e3", List.of())
        );

        List<Finding> consolidated = service.consolidate(findings);

        assertEquals("HIGH", consolidated.get(0).getSeverity());
        assertEquals("MEDIUM", consolidated.get(1).getSeverity());
        assertEquals("LOW", consolidated.get(2).getSeverity());
    }

    @Test
    void consolidateShouldHandleEmptyInput() {
        assertTrue(service.consolidate(List.of()).isEmpty());
        assertTrue(service.consolidate(null).isEmpty());
    }
}

