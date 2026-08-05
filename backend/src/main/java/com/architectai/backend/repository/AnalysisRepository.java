package com.architectai.backend.repository;

import com.architectai.backend.model.Analysis;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, String> {
    @EntityGraph(attributePaths = {"findings", "staticAnalysisFindingsByTool"})
    Optional<Analysis> findDetailedById(String id);

    @EntityGraph(attributePaths = {"findings", "staticAnalysisFindingsByTool"})
    List<Analysis> findDetailedByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Analysis> findByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);
}

